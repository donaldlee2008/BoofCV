/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.core.image.impl;

import boofcv.misc.AutoTypeImage;
import boofcv.misc.CodeGeneratorBase;
import boofcv.misc.CodeGeneratorUtil;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;


/**
 * @author Peter Abeles
 */
public class GenerateImplConvertImage extends CodeGeneratorBase {

	String className = "ImplConvertImage";

	PrintStream out;

	public GenerateImplConvertImage() throws FileNotFoundException {
		out = new PrintStream(new FileOutputStream(className + ".java"));
	}

	@Override
	public void generate() throws FileNotFoundException {
		printPreamble();

		for( AutoTypeImage in : AutoTypeImage.getSpecificTypes()) {
			for( AutoTypeImage out : AutoTypeImage.getGenericTypes() ) {
				if( in == out )
					continue;

				printConvert(in,out);
			}
		}

		out.print("\n" +
				"}\n");
	}

	private void printPreamble() {
		out.print(CodeGeneratorUtil.copyright);
		out.print("package boofcv.core.image.impl;\n" +
				"\n" +
				"import boofcv.alg.InputSanityCheck;\n" +
				"import boofcv.struct.image.*;\n" +
				"\n" +
				"/**\n" +
				" * <p>\n" +
				" * Functions for converting between different primitive image types. Numerical values do not change or are closely approximated\n" +
				" * in these functions.  \n" +
				" * </p>\n" +
				" *\n" +
				" * <p>\n" +
				" * DO NOT MODIFY: This class was automatically generated by {@link boofcv.core.image.impl.GenerateImplConvertImage}\n" +
				" * </p>\n" +
				" *\n" +
				" * @author Peter Abeles\n" +
				" */\n" +
				"public class "+className+" {\n\n");
	}

	private void printConvert( AutoTypeImage imageIn , AutoTypeImage imageOut ) {

		String typeCast = "( "+imageOut.getDataType()+" )";
		String bitWise = imageIn.getBitWise();

		boolean sameTypes = imageIn.getDataType().compareTo(imageOut.getDataType()) == 0;

		if( imageIn.isInteger() && imageOut.isInteger() && imageOut.getNumBits() == 32 )
			typeCast = "";
		else if( sameTypes && imageIn.isSigned() )
			typeCast = "";

		out.print("\tpublic static void convert( "+imageIn.getImageName()+" from, "+imageOut.getImageName()+" to ) {\n" +
				"\n" +
				"\t\tInputSanityCheck.checkSameShape(from, to);\n" +
				"\n" +
				"\t\tif (from.isSubimage() || to.isSubimage()) {\n" +
				"\n" +
				"\t\t\tfor (int y = 0; y < from.height; y++) {\n" +
				"\t\t\t\tint indexFrom = from.getIndex(0, y);\n" +
				"\t\t\t\tint indexTo = to.getIndex(0, y);\n" +
				"\n" +
				"\t\t\t\tfor (int x = 0; x < from.width; x++) {\n" +
				"\t\t\t\t\tto.data[indexTo++] = "+typeCast+"( from.data[indexFrom++] "+bitWise+");\n" +
				"\t\t\t\t}\n" +
				"\t\t\t}\n" +
				"\n" +
				"\t\t} else {\n" +
				"\t\t\tfinal int N = from.width * from.height;\n" +
				"\n");

		if( sameTypes ) {
			out.print("\t\t\tSystem.arraycopy(from.data, 0, to.data, 0, N);\n");
		} else {
			out.print("\t\t\tfor (int i = 0; i < N; i++) {\n" +
					"\t\t\t\tto.data[i] = "+typeCast+"( from.data[i] "+bitWise+");\n" +
					"\t\t\t}\n");
		}
		out.print("\t\t}\n" +
				"\t}\n\n");
	}

	public static void main( String args[] ) throws FileNotFoundException {
		GenerateImplConvertImage app = new GenerateImplConvertImage();

		app.generate();
	}
}
