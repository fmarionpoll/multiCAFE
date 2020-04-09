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
import plugins.fmp.multicafeTools.XLSExportCapillariesResults2;
import plugins.fmp.multicafeTools.XLSExportMoveResults;
import plugins.fmp.multicafeTools.XLSExportOptions;


public class MCExcel_  extends JPanel implements PropertyChangeListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4296207607692017074L;
	private JTabbedPane 	tabsPane 		= new JTabbedPane();
	private MCExcel_Options	tabOptions		= new MCExcel_Options();
	private MCExcel_Kymos	tabKymos		= new MCExcel_Kymos();
	private MCExcel_Move 	tabMove  		= new MCExcel_Move();
	private MultiCAFE parent0 = null;

	
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
		Experiment exp = parent0.expList.getExperiment(parent0.currentExperimentIndex);
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
//		else if (evt.getPropertyName().equals("EXPORT_KYMOSDATA")) {
//			String file = defineXlsFileName(exp, "_feeding.xlsx");
//			if (file == null)
//				return;
//			updateParametersCurrentExperiment(exp);
//			ThreadUtil.bgRun( new Runnable() { @Override public void run() {
//				XLSExportCapillariesResults xlsExport = new XLSExportCapillariesResults();
//				xlsExport.exportToFile(file, getCapillariesOptions());
//			}});
//			firePropertyChange("SAVE_KYMOSMEASURES", false, true);	
//		} 
		else if (evt.getPropertyName().equals("EXPORT_KYMOSDATA2")) {
			String file = defineXlsFileName(exp, "_feeding.xlsx");
			if (file == null)
				return;
			updateParametersCurrentExperiment(exp);
			ThreadUtil.bgRun( new Runnable() { @Override public void run() {
				XLSExportCapillariesResults2 xlsExport2 = new XLSExportCapillariesResults2();
				xlsExport2.exportToFile(file, getCapillariesOptions());
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
		parent0.paneSequence.tabInfos.getExperimentInfosFromDialog(exp);
	}
	
	private XLSExportOptions getMoveOptions() {
		XLSExportOptions options = new XLSExportOptions();
		options.xyCenter 		= tabMove.xyCenterCheckBox.isSelected(); 
		options.distance 		= tabMove.distanceCheckBox.isSelected();
		options.alive 			= tabMove.aliveCheckBox.isSelected(); 
		getCommonOptions(options);
		return options;
	}
	
	private XLSExportOptions getCapillariesOptions() {
		XLSExportOptions options = new XLSExportOptions();
		options.topLevel 		= tabKymos.topLevelCheckBox.isSelected(); 
		options.topLevelDelta   = tabKymos.topLevelDeltaCheckBox.isSelected();
		options.bottomLevel 	= tabKymos.bottomLevelCheckBox.isSelected(); 
		options.derivative 		= tabKymos.derivativeCheckBox.isSelected(); 
		options.consumption 	= tabKymos.consumptionCheckBox.isSelected(); 
		options.sum 			= tabKymos.sumCheckBox.isSelected(); 
		options.t0 				= tabKymos.t0CheckBox.isSelected();
		options.onlyalive 		= tabKymos.onlyaliveCheckBox.isSelected();
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
		if (tabOptions.exportAllFilesCheckBox.isSelected()) {
			options.firstExp 	= 0;
			options.lastExp 	= options.expList.getSize() - 1;
		} else {
			options.firstExp 	= parent0.currentExperimentIndex;
			options.lastExp 	= parent0.currentExperimentIndex;
		}
	}
}
