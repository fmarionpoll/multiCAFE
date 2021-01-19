package plugins.fmp.multicafe.dlg.excel;

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
import icy.system.thread.ThreadUtil;
import plugins.fmp.multicafe.MultiCAFE;
import plugins.fmp.multicafe.sequence.Dialog;
import plugins.fmp.multicafe.sequence.Experiment;
import plugins.fmp.multicafe.sequence.ExperimentList;
import plugins.fmp.multicafe.tools.toExcel.XLSExportCapillariesResults;
import plugins.fmp.multicafe.tools.toExcel.XLSExportGulpsResults;
import plugins.fmp.multicafe.tools.toExcel.XLSExportMoveResults;
import plugins.fmp.multicafe.tools.toExcel.XLSExportOptions;


public class MCExcel_  extends JPanel implements PropertyChangeListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4296207607692017074L;
	public	PopupPanel capPopupPanel	= null;
	private JTabbedPane 	tabsPane 		= new JTabbedPane();
	public Options			tabOptions		= new Options();
	private Levels			tabKymos		= new Levels();
	private Gulps			tabGulps		= new Gulps();
	private Move 			tabMove  		= new Move();
	private MultiCAFE 		parent0 = null;

	
	public void init (JPanel mainPanel, String string, MultiCAFE parent0) {
		this.parent0 = parent0;
		
		capPopupPanel = new PopupPanel(string);
		JPanel capPanel = capPopupPanel.getMainPanel();
		capPanel.setLayout(new BorderLayout());
		capPopupPanel.collapse();
		mainPanel.add(capPopupPanel);
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
		
		capPanel.add(tabsPane);
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
				XLSExportGulpsResults xlsExport2 = new XLSExportGulpsResults();
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
		return Dialog.saveFileAs(tentativeName, directory.getParent().toString(), "xlsx");
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
		options.sumGulps 		= false; 
		options.isGulps 		= false;
		
		options.topLevel 		= tabKymos.topLevelCheckBox.isSelected(); 
		options.topLevelDelta   = tabKymos.topLevelDeltaCheckBox.isSelected();
		options.bottomLevel 	= tabKymos.bottomLevelCheckBox.isSelected(); 
		options.derivative 		= tabKymos.derivativeCheckBox.isSelected(); 
		options.sumGulps 		= false; 
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
		options.cage 			= false;
		options.t0 				= false;
		options.sumGulps 		= tabGulps.sumGulpsCheckBox.isSelected(); 
		options.sum_ratio_LR 	= tabGulps.sumCheckBox.isSelected(); 
		options.onlyalive 		= tabGulps.onlyaliveCheckBox.isSelected();
		options.isGulps 		= tabGulps.isGulpsCheckBox.isSelected();
		options.tToNextGulp		= tabGulps.tToGulpCheckBox.isSelected();
		options.tToNextGulp_LR  = tabGulps.tToGulpLRCheckBox.isSelected();
		options.subtractEvaporation = false;
		getCommonOptions(options);
		return options;
	}
	
	private void getCommonOptions(XLSExportOptions options) {
		options.transpose 		= tabOptions.transposeCheckBox.isSelected();
		options.buildExcelStepMs = tabOptions.getExcelBuildStep() ;
		options.buildExcelUnitMs = tabOptions.binUnit.getMsUnitValue();
		options.fixedIntervals 	= tabOptions.isFixedFrame.isSelected();
		options.startAll_Ms 	= tabOptions.getStartAllMs();
		options.endAll_Ms 		= tabOptions.getEndAllMs();
		
		options.collateSeries 	= tabOptions.collateSeriesCheckBox.isSelected();
		options.padIntervals 	= tabOptions.padIntervalsCheckBox.isSelected();
		options.absoluteTime	= tabOptions.absoluteTimeCheckBox.isSelected();
		options.exportAllFiles 	= tabOptions.exportAllFilesCheckBox.isSelected();
		options.expList = new ExperimentList(); 
		parent0.paneSequence.transferExperimentNamesToExpList(options.expList, true);
		options.expList.expListResultsSubPath = (String) parent0.paneKymos.tabDisplay.availableResultsCombo.getSelectedItem() ;
		if (tabOptions.exportAllFilesCheckBox.isSelected()) {
			options.firstExp 	= 0;
			options.lastExp 	= options.expList.getExperimentListSize() - 1;
		} else {
			options.firstExp 	= parent0.expList.currentExperimentIndex;
			options.lastExp 	= parent0.expList.currentExperimentIndex;
		}
	}
}
