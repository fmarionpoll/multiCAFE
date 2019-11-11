package plugins.fmp.multicafe;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import icy.gui.component.PopupPanel;
import icy.gui.util.GuiUtil;
import icy.system.thread.ThreadUtil;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeSequence.ExperimentList;
import plugins.fmp.multicafeTools.MulticafeTools;
import plugins.fmp.multicafeTools.XLSExportCapillariesResults;
import plugins.fmp.multicafeTools.XLSExportMoveResults;
import plugins.fmp.multicafeTools.XLSExportOptions;


public class MCExcel_  extends JPanel implements PropertyChangeListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4296207607692017074L;
	private JTabbedPane 	tabsPane 		= new JTabbedPane();
	private MCExcel_Options	optionsTab		= new MCExcel_Options();
	private MCExcel_Kymos	kymosTab		= new MCExcel_Kymos();
	private MCExcel_Move 	moveTab  		= new MCExcel_Move();
	
	private MultiCAFE parent0 = null;

	void init (JPanel mainPanel, String string, MultiCAFE parent0) {
		this.parent0 = parent0;
		
		PopupPanel capPopupPanel = new PopupPanel(string);
		JPanel capPanel = capPopupPanel.getMainPanel();
		capPanel.setLayout(new BorderLayout());
		capPopupPanel.collapse();
		mainPanel.add(GuiUtil.besidesPanel(capPopupPanel));
		GridLayout capLayout = new GridLayout(3, 2);
		
		optionsTab.init(capLayout);
		tabsPane.addTab("Common options", null, optionsTab, "Define common options");
		optionsTab.addPropertyChangeListener(this);
		
		kymosTab.init(capLayout);
		tabsPane.addTab("Capillaries", null, kymosTab, "Export capillary levels to file");
		kymosTab.addPropertyChangeListener(this);
		
		moveTab.init(capLayout);
		tabsPane.addTab("Move", null, moveTab, "Export fly positions to file");
		moveTab.addPropertyChangeListener(this);
		
		capPanel.add(GuiUtil.besidesPanel(tabsPane));
		tabsPane.setSelectedIndex(0);
		
		capPopupPanel.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				parent0.mainFrame.revalidate();
				parent0.mainFrame.pack();
				parent0.mainFrame.repaint();
			}
		});
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		
		Experiment exp = parent0.expList.getExperiment(parent0.currentIndex);
		if (exp == null) 
			return;
		
		if (evt.getPropertyName().equals("EXPORT_MOVEDATA")) {
			String file = defineXlsFileName(exp, "_move.xlsx");
			if (file == null)
				return;
			updateParametersCurrentExperiment(exp);
			ThreadUtil.bgRun( new Runnable() { @Override public void run() {
				XLSExportMoveResults xlsExport = new XLSExportMoveResults();
				xlsExport.exportToFile(file, getMoveOptions());
			}});
		}
		else if (evt.getPropertyName().equals("EXPORT_KYMOSDATA")) {
			String file = defineXlsFileName(exp, "_feeding.xlsx");
			if (file == null)
				return;
			updateParametersCurrentExperiment(exp);
			ThreadUtil.bgRun( new Runnable() { @Override public void run() {
				XLSExportCapillariesResults xlsExport = new XLSExportCapillariesResults();
				xlsExport.exportToFile(file, getCapillariesOptions());
			}});
			firePropertyChange("SAVE_KYMOSMEASURES", false, true);	
		}
	}
	
	private String defineXlsFileName(Experiment exp, String pattern) {
		String filename0 = exp.seqCamData.getFileName(0);
		Path directory = Paths.get(filename0).getParent();
		Path subpath = directory.getName(directory.getNameCount()-1);
		String tentativeName = subpath.toString()+ pattern;
		return MulticafeTools.saveFileAs(tentativeName, directory.getParent().toString(), "xlsx");
	}
	
	private void updateParametersCurrentExperiment(Experiment exp) {
		parent0.capillariesPane.getCapillariesInfos(exp.seqKymos.capillaries);
		parent0.sequencePane.infosTab.getExperimentInfosFromDialog(exp);
	}
	
	private XLSExportOptions getMoveOptions() {
		XLSExportOptions options = new XLSExportOptions();
		options.xyCenter 		= moveTab.xyCenterCheckBox.isSelected(); 
		options.distance 		= moveTab.distanceCheckBox.isSelected();
		options.alive 			= moveTab.aliveCheckBox.isSelected(); 
		getCommonOptions(options);
		return options;
	}
	
	private XLSExportOptions getCapillariesOptions() {
		XLSExportOptions options = new XLSExportOptions();
		
		options.topLevel 		= kymosTab.topLevelCheckBox.isSelected(); 
		options.topLevelDelta   = kymosTab.topLevelDeltaCheckBox.isSelected();
		options.bottomLevel 	= kymosTab.bottomLevelCheckBox.isSelected(); 
		options.derivative 		= kymosTab.derivativeCheckBox.isSelected(); 
		options.consumption 	= kymosTab.consumptionCheckBox.isSelected(); 
		options.sum 			= kymosTab.sumCheckBox.isSelected(); 
		options.t0 				= kymosTab.t0CheckBox.isSelected();
		options.onlyalive 		= kymosTab.onlyaliveCheckBox.isSelected();

		getCommonOptions(options);
		return options;
	}
	
	private void getCommonOptions(XLSExportOptions options) {
		options.pivot 			= optionsTab.pivotCheckBox.isSelected();
		options.transpose 		= true;
		if (!options.pivot) 
			options.transpose 	= optionsTab.transposeCheckBox.isSelected();
		options.pivotBinStep = (int) optionsTab.pivotBinStep.getValue();
		options.collateSeries 	= optionsTab.collateSeriesCheckBox.isSelected();
		options.padIntervals 	= optionsTab.padIntervalsCheckBox.isSelected();
		
		options.absoluteTime	= optionsTab.absoluteTimeCheckBox.isSelected();
		options.exportAllFiles 	= optionsTab.exportAllFilesCheckBox.isSelected();
		options.expList = new ExperimentList(); 
		parent0.sequencePane.infosTab.transferExperimentNamesToExpList(options.expList);		
		if (optionsTab.exportAllFilesCheckBox.isSelected()) {
			options.firstExp 	= 0;
			options.lastExp 	= options.expList.experimentList.size() - 1;
		} else {
			options.firstExp 	= parent0.currentIndex;
			options.lastExp 	= parent0.currentIndex;
		}
	}
}
