package org.uet.emftouse.actions;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.tzi.use.config.Options;
import org.tzi.use.gui.main.MainWindow;
import org.tzi.use.gui.main.ModelBrowser;
import org.tzi.use.gui.util.ExtFileFilter;
import org.tzi.use.main.Session;
import org.tzi.use.runtime.gui.IPluginAction;
import org.tzi.use.runtime.gui.IPluginActionDelegate;
import org.tzi.use.runtime.gui.impl.PluginMMPrintVisitor;
import org.tzi.use.uml.mm.MModel;

public class ActionSave implements IPluginActionDelegate {
	private Session mySession;

	public void performAction(IPluginAction pluginAction) {
		// Get current session
		mySession = pluginAction.getSession();
		try{
			// Get the current model
			MModel newModel = mySession.system().model();
			// Get current main window
			MainWindow fMainWindow = pluginAction.getParent();
			// Select location to save
			Path path = Options.getLastDirectory();
			JFileChooser fChooser = new JFileChooser(path.toString());
			fChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fChooser.setFileFilter(new ExtFileFilter("use"));
			fChooser.setCurrentDirectory(path.toFile());
			fChooser.setDialogTitle("Select location to save");
			int returnVal = fChooser.showSaveDialog(fMainWindow);
			if (returnVal != JFileChooser.APPROVE_OPTION) {
				return;
			}
			// Get string specification .use of the model
			ModelBrowser mb = fMainWindow.getModelBrowser();
			StringWriter sw = new StringWriter();
			PluginMMPrintVisitor p = new PluginMMPrintVisitor(new PrintWriter(sw), mb);
			newModel.processWithVisitor(p);
		    String spec = sw.toString();
		    // Get the selected file
			File selectedFile = fChooser.getSelectedFile();
			Options.setLastDirectory(selectedFile.toPath());
			try {
				// Create file at the directory
	            FileWriter fw = new FileWriter(selectedFile.getAbsolutePath() +".use");
	            // Write data to file
	            fw.write(spec);
	            // Save file
	            fw.close();
	        } catch (Exception ex) {
	            ex.printStackTrace();
	        }
		}catch(IllegalStateException e){
			// Get current main window
			MainWindow fMainWindow = pluginAction.getParent();
			JOptionPane.showMessageDialog(fMainWindow, "You don't have a model, please load a model.");
		}
	}
}