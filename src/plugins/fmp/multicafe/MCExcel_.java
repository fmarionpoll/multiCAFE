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
	MCExcel_Options			tabOptions		= new MCExcel_Options();
	private MCExcel_Levels	tabKymos		= new MCExcel_Levels();
	private MCExcel_Gulps	tabGulps		= new MCExcel_Gulps();
	private MCExcel_Move 	tabMove  		= new MCExcel_Move();
	private MultiCAFE 		parent0 = null;

	
	void init (JPanel mainPanel, String string, MultiCAFE parent0) {
		this.parent0 = parent0;
		
		PopupPanel capPopupPanel = new PopupPanel(string);
		JPanel capPanel = capPopupPanel.getMainPanel();
		capPanel.setLayout(new BorderLayout());
		capPopupPanel.collapse();
		mainPanel.add(GuiUtil.besidesPanel(capPopupPanel));
		GridLayout capLayout = new GridLayout(3, 2);
		
		tabOptions.init(capLayout);
		tabsPane.addTab("Common options", null, tabOptions, "Define common options");
		tabOptions.addPropertyChangeListener(this);
		
		tabKymos.init(capLayout);
		tabsPane.addTab("Capillaries", null, tabKymos, "Export capillary levels to file");
		tabKymos.addPropertyChangeListener(this);
		
		tabGulps.init(capLayout);
		tabsPane.addTab("Gulps", null, tabGulps, "Export gulps to file");
		tabGulps.addPropertyChangeListener(this);
		
		
		tabMove.init(capLayout);
		tabsPane.addTab("Move", null, tabMove, "Export fly positions to file");
		tabMove.addPropertyChangeListener(this);
		
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
		Experiment exp = parent0.expList.getCurrentExperiment();
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
				XLSExportCapillariesResults xlsExport2 = new XLSExportCapillariesResults();
				xlsExport2.exportToFile(file, getLevelsOptions());
			}});
			firePropertyChange("SAVE_KYMOSMEASURES", false, true);	
		}
		else if (evt.getPropertyName().equals("EXPORT_GULPSDATA")) {
			String file = defineXlsFileName(exp, "_gulps.xlsx");
			if (file == null)
				return;
			updateParametersCurrentExperiment(exp);
			ThreadUtil.bgRun( new Runnable() { @Override public void run() {
				XLSExportCapillariesResults xlsExport2 = new XLSExportCapillariesResults();
				xlsExport2.exportToFile(file, getGulpsOptions());
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
		parent0.paneCapillaries.getCapillariesInfos(exp);
		parent0.paneSequence.tabInfosSeq.getExperimentInfosFromDialog(exp);
	}
	
	private XLSExportOptions getMoveOptions() {
		XLSExportOptions options = new XLSExportOptions();
		options.xyImage 		= tabMove.xyCenterCheckBox.isSelected(); 
		options.xyTopCage		= tabMove.xyCageCheckBox.isSelected();
		options.xyTipCapillaries= tabMove.xyTipCapsCheckBox.isSelected();
		options.distance 		= tabMove.distanceCheckBox.isSelected();
		options.alive 			= tabMove.aliveCheckBox.isSelected(); 
		options.onlyalive 		= tabMove.deadEmptyCheckBox.isSelected();
		options.sleep			= tabMove.sleepCheckBox.isSelected();
		getCommonOptions(options);
		return options;
	}
	
	private XLSExportOptions getLevelsOptions() {
		XLSExportOptions options = new XLSExportOptions();
		options.topLevel 		= tabKymos.topLevelCheckBox.isSelected(); 
		options.topLevelDelta   = tabKymos.topLevelDeltaCheckBox.isSelected();
		options.bottomLevel 	= tabKymos.bottomLevelCheckBox.isSelected(); 
		options.derivative 		= tabKymos.derivativeCheckBox.isSelected(); 
		options.consumption 	= false; 
		options.sum_ratio_LR 	= tabKymos.sumCheckBox.isSelected(); 
		options.cage 			= tabKymos.cageCheckBox.isSelected();
		options.t0 				= tabKymos.t0CheckBox.isSelected();
		options.onlyalive 		= tabKymos.onlyaliveCheckBox.isSelected();
		options.subtractEvaporation = tabKymos.subtractEvaporationCheckBox.isSelected();
		getCommonOptions(options);
		return options;
	}
	
	private XLSExportOptions getGulpsOptions() {
		XLSExportOptions options = new XLSExportOptions();
		options.topLevel 		= false; 
		options.topLevelDelta   = false;
		options.bottomLevel 	= false; 
		options.derivative 		= false; 
		options.consumption 	= tabGulps.sumGulpsCheckBox.isSelected(); 
		options.sum_ratio_LR 	= false; 
		options.cage 			= false;
		options.t0 				= false;
		options.onlyalive 		= tabGulps.onlyaliveCheckBox.isSelected();
		options.subtractEvaporation = false;
		getCommonOptions(options);
		return options;
	}
	
	private void getCommonOptions(XLSExportOptions options) {
		options.transpose 	= tabOptions.transposeCheckBox.isSelected();
		options.buildExcelBinStep = (int) tabOptions.pivotBinStep.getValue();
		options.collateSeries 	= tabOptions.collateSeriesCheckBox.isSelected();
		options.padIntervals 	= tabOptions.padIntervalsCheckBox.isSelected();
		options.absoluteTime	= tabOptions.absoluteTimeCheckBox.isSelected();
		options.exportAllFiles 	= tabOptions.exportAllFilesCheckBox.isSelected();
		options.expList = new ExperimentList(); 
		parent0.paneSequence.transferExperimentNamesToExpList(options.expList, true);
		options.expList.expListResultsSubPath = (String) parent0.paneKymos.tabDisplay.availableResultsCombo.getSelectedItem() ;
		if (tabOptions.exportAllFilesCheckBox.isSelected()) {
			options.firstExp 	= 0;
			options.lastExp 	= options.expList.getSize() - 1;
		} else {
			options.firstExp 	= parent0.expList.currentExperimentIndex;
			options.lastExp 	= parent0.expList.currentExperimentIndex;
		}
	}
}
