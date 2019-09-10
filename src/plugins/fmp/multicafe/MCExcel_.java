package plugins.fmp.multicafe;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import icy.gui.component.PopupPanel;
import icy.gui.util.GuiUtil;
import icy.system.thread.ThreadUtil;
import plugins.fmp.multicafeSequence.Experiment;
import plugins.fmp.multicafeTools.MulticafeTools;
import plugins.fmp.multicafeTools.XLSExportCapillaryResults;
import plugins.fmp.multicafeTools.XLSExportMoveResults;
import plugins.fmp.multicafeTools.XLSExportOptions;


public class MCExcel_  extends JPanel implements PropertyChangeListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4296207607692017074L;
	private JTabbedPane 		tabsPane 		= new JTabbedPane();
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

		if (evt.getPropertyName().equals("EXPORT_MOVEDATA")) {
			Path directory = Paths.get(exp.seqCamData.getFileName(0)).getParent();
			Path subpath = directory.getName(directory.getNameCount()-1);
			String tentativeName = subpath.toString()+"_move.xlsx";
			String file = MulticafeTools.saveFileAs(tentativeName, directory.getParent().toString(), "xlsx");
			if (file != null) {
				final String filename = file;
				parent0.capillariesPane.getCapillariesInfos(exp.seqKymos.capillaries);
				parent0.sequencePane.infosTab.getCapillariesInfosFromDialog(exp.seqKymos.capillaries);
				ThreadUtil.bgRun( new Runnable() { @Override public void run() {
					XLSExportMoveResults xlsExport = new XLSExportMoveResults();
					xlsExport.exportToFile(filename, getMoveOptions());
				}});
			}
		}
		else if (evt.getPropertyName().equals("EXPORT_KYMOSDATA")) {
			String filename0 = exp.seqCamData.getFileName(0);
			Path directory = Paths.get(filename0).getParent();
			Path subpath = directory.getName(directory.getNameCount()-1);
			String tentativeName = subpath.toString()+"_feeding.xlsx";
			String file = MulticafeTools.saveFileAs(tentativeName, directory.getParent().toString(), "xlsx");
			if (file != null) {
				final String filename = file;
				parent0.capillariesPane.getCapillariesInfos(exp.seqKymos.capillaries);
				parent0.sequencePane.infosTab.getCapillariesInfosFromDialog(exp.seqKymos.capillaries);
				ThreadUtil.bgRun( new Runnable() { @Override public void run() {
					XLSExportCapillaryResults xlsExport = new XLSExportCapillaryResults();
					xlsExport.exportToFile(filename, getCapillariesOptions());
				}});
				firePropertyChange("EXPORT_TO_EXCEL", false, true);	
			}
		}
	}
	
	private XLSExportOptions getMoveOptions() {
		XLSExportOptions options = new XLSExportOptions();
		options.xyCenter = moveTab.xyCenterCheckBox.isSelected(); 
		options.distance = moveTab.distanceCheckBox.isSelected();
		options.alive = moveTab.aliveCheckBox.isSelected(); 
		getCommonOptions(options);
		return options;
	}
	
	private void getCommonOptions(XLSExportOptions options) {
		options.pivot 			= optionsTab.pivotCheckBox.isSelected();
		if (options.pivot) {
			options.transpose = true;
			try {
				optionsTab.pivotBinStep.commitEdit();
			} catch (ParseException e) {
				e.printStackTrace();
			}
			options.pivotBinStep = (int) optionsTab.pivotBinStep.getValue();
		}
		else {
			options.transpose 	= optionsTab.transposeCheckBox.isSelected();
		}
		options.collateSeries 	= optionsTab.collateSeriesCheckBox.isSelected();
		options.absoluteTime	= optionsTab.absoluteTimeCheckBox.isSelected();
		options.exportAllFiles 	= optionsTab.exportAllFilesCheckBox.isSelected();
		if (optionsTab.exportAllFilesCheckBox.isSelected()) {
			//parent0.sequencePane.infosTab.transferExperimentNamesToExpList(parent0.expList);
			int nfiles = parent0.expList.experimentList.size();
			options.firstExp = 0;
			options.lastExp = nfiles - 1;
		}
		else {
			options.firstExp = parent0.currentIndex;
			options.lastExp = parent0.currentIndex;
		}
		options.expList = parent0.expList;
	}
	
	private XLSExportOptions getCapillariesOptions() {
		XLSExportOptions options = new XLSExportOptions();
		
		options.topLevel 		= kymosTab.topLevelCheckBox.isSelected(); 
		options.topLevelDelta 	= kymosTab.topLevelDCheckBox.isSelected(); 	
		options.bottomLevel 	= kymosTab.bottomLevelCheckBox.isSelected(); 
		options.derivative 		= kymosTab.derivativeCheckBox.isSelected(); 
		options.consumption 	= kymosTab.consumptionCheckBox.isSelected(); 
		options.sum 			= kymosTab.sumCheckBox.isSelected(); 
		options.t0 				= kymosTab.t0CheckBox.isSelected();
		options.onlyalive 		= kymosTab.onlyaliveCheckBox.isSelected();

		getCommonOptions(options);
		return options;
	}
}
