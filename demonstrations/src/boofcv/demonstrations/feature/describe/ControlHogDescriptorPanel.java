/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.demonstrations.feature.describe;

import boofcv.gui.StandardAlgConfigPanel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Panel for adjusting how HOG is visualized and configured
 *
 * @author Peter Abeles
 */
public class ControlHogDescriptorPanel extends StandardAlgConfigPanel
	implements ChangeListener, ItemListener
{
	JCheckBox showGrid;
	JCheckBox useFast;


	JSpinner selectWidth;
	JSpinner selectHistogram;
	JSpinner selectGridX;
	JSpinner selectGridY;

	boolean doShowGrid = true;

	int cellWidth = 20;
	int histogram = 9;
	int gridX = 3;
	int gridY = 5;
	boolean fast = true;

	VisualizeHogDescriptorApp owner;

	public ControlHogDescriptorPanel(VisualizeHogDescriptorApp owner) {

		this.owner = owner;

		showGrid = new JCheckBox("Show Grid");
		showGrid.setSelected(doShowGrid);
		showGrid.addItemListener(this);
		showGrid.setMaximumSize(showGrid.getPreferredSize());

		useFast = new JCheckBox("Fast HOG");
		useFast.setSelected(fast);
		useFast.addItemListener(this);
		useFast.setMaximumSize(useFast.getPreferredSize());

		selectWidth = new JSpinner(new SpinnerNumberModel(cellWidth, 5, 50, 1));
		selectWidth.addChangeListener(this);
		selectWidth.setMaximumSize(selectWidth.getPreferredSize());

		selectHistogram = new JSpinner(new SpinnerNumberModel(histogram, 4, 32, 1));
		selectHistogram.addChangeListener(this);
		selectHistogram.setMaximumSize(selectHistogram.getPreferredSize());

		selectGridX = new JSpinner(new SpinnerNumberModel(gridX, 1, 20, 1));
		selectGridX.addChangeListener(this);
		selectGridX.setMaximumSize(selectGridX.getPreferredSize());

		selectGridY = new JSpinner(new SpinnerNumberModel(gridY, 1, 20, 1));
		selectGridY.addChangeListener(this);
		selectGridY.setMaximumSize(selectGridY.getPreferredSize());


		addAlignLeft(showGrid, this);
		addAlignLeft(useFast,this);

		addLabeled(selectWidth, "Cell Size:", this);
		addLabeled(selectGridX, "Grid X:", this);
		addLabeled(selectGridY, "Grid Y:", this);
		addLabeled(selectHistogram, "Histogram:", this);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if( selectWidth == e.getSource() ) {
			cellWidth = ((Number) selectWidth.getValue()).intValue();
			owner.configChanged();
		} else if( selectHistogram == e.getSource() ) {
			histogram = ((Number) selectHistogram.getValue()).intValue();
			owner.configChanged();
		} else if( selectGridX == e.getSource() ) {
			gridX = ((Number) selectGridX.getValue()).intValue();
			owner.configChanged();
		} else if( selectGridY == e.getSource() ) {
			gridY = ((Number) selectGridY.getValue()).intValue();
			owner.configChanged();
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if( showGrid == e.getSource() ) {
			doShowGrid = showGrid.isSelected();
			owner.visualsChanged();
		} else if( useFast == e.getSource() ) {
			fast = useFast.isSelected();
			owner.configChanged();
		}
	}
}
