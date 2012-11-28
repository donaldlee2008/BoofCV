/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.alg.feature.detect.interest;

import boofcv.alg.filter.convolve.ConvolveNormalized;
import boofcv.alg.misc.PixelMath;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.image.ImageFloat32;

/**
 * Constructs the scale-space in which SIFT detects features.  An octave contains a set of scales.
 * Each octave is half the width/height of the previous octave.  Scales are computed inside an octave
 * by applying Gaussian blur.   See SIFT paper for the details.
 *
 * Only one octave is computed at a time.  Higher octaves are computed by calling {@link #computeNextOctave()}
 * and overwrite the previous scales.
 *
 * @author Peter Abeles
 */
public class SiftImageScaleSpace {

	// Storage for Difference of Gaussian (DOG) features
	protected ImageFloat32 dog[];
	// Images across scale-space in this octave
	protected ImageFloat32 scale[];
	// Amount of blur which is applied
	protected float sigma;

	// ratio of pixels in current octave to the original image
	// x = x'*pixelScale, where x is original coordinate, and x' is current image.
	protected double pixelScale;

	// should the input image be doubled
	private boolean doubleInputImage;

	// The blur sigma applied to the first scale BEFORE any additional blur has been applied
	// Note that the octave's are recursively computed, so this is the blur magnitude from before
	private double priorSigmaFirstScale;

	// storage for applying blur
	protected ImageFloat32 storage;

	/**
	 * Configures the scale-space
	 *
	 * @param numScales Number of scales per octave
	 * @param blurSigma Amount of Gaussian blur applied to each scale
	 * @param doubleInputImage Is the input image doubled or not
	 */
	public SiftImageScaleSpace( int numScales , float blurSigma , boolean doubleInputImage)
	{
		if( numScales < 3 )
			throw new IllegalArgumentException("A minimum of 3 scales are required");

		this.sigma = blurSigma;
		this.doubleInputImage = doubleInputImage;

		scale = new ImageFloat32[numScales];
		dog = new ImageFloat32[numScales-1];
		for( int i = 0; i < numScales; i++ ) {
			scale[i] = new ImageFloat32(1,1);
		}
		for( int i = 0; i < numScales-1; i++ ) {
			dog[i] = new ImageFloat32(1,1);
		}
		storage = new ImageFloat32(1,1);
	}

	/**
	 * Processes the first image and constructs the scale-space pyramid for the first level.
	 *
	 * @param input Input image
	 */
	public void process( ImageFloat32 input ) {

		priorSigmaFirstScale = 0;

		if( doubleInputImage ) {
			pixelScale = 0.5;
			reshapeImages(input.width*2,input.height*2);
			upSample(input,scale[1]);

			blurImage(scale[1],scale[0],sigma);
		} else {
			pixelScale = 1;

			reshapeImages(input.width,input.height);
			blurImage(input,scale[0],sigma);
		}

		constructRestOfPyramid();
	}

	/**
	 * Returns the amount of blur which has been applied to the image in total at the specified scale
	 * in the current octave
	 */
	public double computeScaleSigma( int scale ) {
		// amount of blur applied to this octave at the specified level
		double b = pixelScale*sigma*(scale+1);

		// Compute the net effect of convolving b on top of the previous blur amount
		return Math.sqrt(priorSigmaFirstScale * priorSigmaFirstScale + b*b);
	}

	/**
	 * Applies the specified amount of blur to the input image and stores the results in
	 * the output image
	 */
	private void blurImage( ImageFloat32 input , ImageFloat32 output , double sigma ) {
		Kernel1D_F32 kernel = FactoryKernelGaussian.gaussian(Kernel1D_F32.class, sigma, -1);

		ConvolveNormalized.horizontal(kernel, input, storage);
		ConvolveNormalized.vertical(kernel,storage,output);
	}

	/**
	 * Compute difference of Gaussian feature intensity across scale space
	 */
	public void computeFeatureIntensity() {
		for( int i = 1; i < scale.length; i++ ) {
			PixelMath.subtract(scale[i],scale[i-1],dog[i-1]);
			// compute adjustment to make it better approximate of the Laplacian of Gaussian detector
			double k = (i+1)/(double)i;
			double adjustment = (k-1)*sigma*sigma;
			PixelMath.divide(dog[i - 1], (float) adjustment, dog[i - 1]);
		}
	}

	/**
	 * Construct scale-space pyramid in the next octave by sub-sampling the second scale in
	 * the previous octave  and applying additional blur.
	 *
	 * @return true if the image is large enough to process or false if it should not be processed
	 */
	public boolean computeNextOctave() {
		// the second level is selected to seed the next octave
		priorSigmaFirstScale = computeScaleSigma(1);

		pixelScale *= 2;

		int w = scale[0].width/2;
		int h = scale[0].height/2;

		// no points in processing images that are smaller
		if( w < 3 || h < 3 )
			return false;


		scale[0].reshape(w, h);
		downSample(scale[1],scale[0]);

		reshapeImages(w,h);

		constructRestOfPyramid();

		return true;
	}

	/**
	 * Using the first level as seed, construct the rest of the image pyramid in the octave.
	 */
	private void constructRestOfPyramid() {
		for( int i = 1; i < scale.length; i++ ) {
			// sigmaA is the amount of blur already applied
			double sigmaA = sigma*i;
			// sigmaB is the desired amount of blur at this scale
			double sigmaB = sigma*(i+1);

			// compute the amount of blur which needs to be applied to get sigmaB
			double amount = Math.sqrt(sigmaB*sigmaB - sigmaA*sigmaA);

			// apply the blur
			blurImage(scale[i-1],scale[i],amount);
		}
	}

	/**
	 * Down samples an image by copying every other pixel, starting with pixel 1.
	 */
	protected static void downSample( ImageFloat32 from , ImageFloat32 to ) {

		for( int y = 0; y < to.height; y++ ) {
			for( int x = 0; x < to.width; x++ ) {
				to.unsafe_set(x,y,from.unsafe_get(x*2+1,y*2+1));
			}
		}
	}

	/**
	 * Up-samples the input image.  Doubling its size.
	 */
	protected static void upSample( ImageFloat32 from , ImageFloat32 to ) {

		for( int y = 0; y < from.height; y++ ) {
			int yy = y*2;
			int xx = 0;
			for( int x = 0; x < from.width; x++ ) {
				float v = from.unsafe_get(x,y);

				to.unsafe_set(xx, yy, v);
				to.unsafe_set(xx,yy+1,v);
				xx++;
				to.unsafe_set(xx,yy,v);
				to.unsafe_set(xx,yy+1,v);
				xx++;
			}
		}
	}

	/**
	 * Reshapes all images to the specified size
	 */
	private void reshapeImages(int width , int height ) {
		for( int i = 0; i < scale.length; i++ ) {
			scale[i].reshape(width,height);
		}
		for( int i = 0; i < dog.length; i++ ) {
			dog[i].reshape(width, height);
		}
		storage.reshape(width,height);
	}

}
