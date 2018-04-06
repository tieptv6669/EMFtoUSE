package org.uet.emftouse.actions;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.swing.JFileChooser;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAnnotation;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.tzi.use.api.UseApiException;
import org.tzi.use.api.UseModelApi;
import org.tzi.use.config.Options;
import org.tzi.use.gui.main.MainWindow;
import org.tzi.use.gui.util.ExtFileFilter;
import org.tzi.use.main.Session;
import org.tzi.use.runtime.gui.IPluginAction;
import org.tzi.use.runtime.gui.IPluginActionDelegate;
import org.tzi.use.uml.mm.MAggregationKind;
import org.tzi.use.uml.mm.MAssociation;
import org.tzi.use.uml.mm.MAssociationEnd;
import org.tzi.use.uml.mm.MAttribute;
import org.tzi.use.uml.mm.MClass;
import org.tzi.use.uml.mm.MGeneralization;
import org.tzi.use.uml.mm.MInvalidModelException;
import org.tzi.use.uml.mm.MModel;
import org.tzi.use.uml.mm.MMultiplicity;
import org.tzi.use.uml.mm.ModelFactory;
import org.tzi.use.uml.ocl.type.EnumType;
import org.tzi.use.uml.ocl.type.TypeFactory;
import org.tzi.use.uml.sys.MSystem;

public class ActionImport implements IPluginActionDelegate {
	private Session mySession;

	public void performAction(IPluginAction pluginAction) {
		// Select file ecore
		MainWindow fMainWindow = pluginAction.getParent();
		Path path = Options.getLastDirectory();
		JFileChooser fChooser = new JFileChooser(path.toString());
		fChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fChooser.setFileFilter(new ExtFileFilter("ecore"));
		fChooser.setCurrentDirectory(path.toFile());
		fChooser.setDialogTitle("Select .ecore file to import");
		int returnVal = fChooser.showOpenDialog(fMainWindow);
		if (returnVal != JFileChooser.APPROVE_OPTION) {
			return;
		}
		File selectedFile = fChooser.getSelectedFile();
		Options.setLastDirectory(selectedFile.toPath());
		
		// Get current session
		mySession = pluginAction.getSession();
		// Get log writer
		PrintWriter logWriter = pluginAction.getParent().logWriter();
		
		/*
		// Get all class of use model
		Collection<MClass> allClass = mySession.system().model().classes();
		for (MClass cls : allClass) {
			// Show all class of the model
			logWriter.print(cls.toString());
		}
		*/
		/*SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				mySession.setSystem(mySystem);	
				
			}
		});*/
		
		ResourceSet resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());
		
		Resource myMetaModel = resourceSet.getResource(URI.createFileURI(selectedFile.getPath()), true);
		
		EPackage univEPackage = (EPackage) myMetaModel.getContents().get(0);
		resourceSet.getPackageRegistry().put("abc", univEPackage);
		
		// Create new model factory in USE
		ModelFactory mf = new ModelFactory();
		// Create new model in USE
		MModel newModel = mf.createModel(univEPackage.getName());
		// Get all elements of ecore model
		EList<EObject> listElement = univEPackage.eContents();
		// Examine each element
		for(EObject x : listElement){
			// Convert all class
			if(x.eClass().getName() == "EClass"){
				EClass temp = (EClass) x;
				
				// Get all attribute of the class in ecore model
				EList<EAttribute> eattri = temp.getEAllAttributes();
				
				// Create new class in USE
				MClass newClass;
								
				if(temp.isAbstract()){
					newClass = mf.createClass(temp.getName(), true);
				}else{
					newClass = mf.createClass(temp.getName(), false);
				}
				
				// Add class into model in USE
				try {
					newModel.addClass(newClass);
				} catch (MInvalidModelException e) {
					e.printStackTrace();
				}
				
				// Examine each attribute in the list attribute
				for(EAttribute item : eattri){
					// Get name of attribute
					String nameOfAttri = item.getName();
					// Get data type of attribute
					String dataType = item.getEAttributeType().getName();
					// Create new attribute and add into class in use (Convert attribute)
					if(dataType == "EString"){
						MAttribute ma = mf.createAttribute(nameOfAttri, TypeFactory.mkString());
						try {
							newClass.addAttribute(ma);
						} catch (MInvalidModelException e) {
							e.printStackTrace();
						}
					}
					if(dataType == "EInt" || dataType == "ELong" || dataType == "EShort"){
						MAttribute ma = mf.createAttribute(nameOfAttri, TypeFactory.mkInteger());
						try {
							newClass.addAttribute(ma);
						} catch (MInvalidModelException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					if(dataType == "EDouble" || dataType == "EFloat"){
						MAttribute ma = mf.createAttribute(nameOfAttri, TypeFactory.mkReal());
						try {
							newClass.addAttribute(ma);
						} catch (MInvalidModelException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					if(dataType == "EBoolean"){
						MAttribute ma = mf.createAttribute(nameOfAttri, TypeFactory.mkBoolean());
						try {
							newClass.addAttribute(ma);
						} catch (MInvalidModelException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
				}
				
			}
			
			// Convert all enum type
			if(x.eClass().getName() == "EEnum"){
				EEnum temp = (EEnum) x;
				String nameOfEnum = temp.getName();
				ArrayList<String> literals = new ArrayList<String>();
				EList<EObject> listLiterals = temp.eContents();
				for(EObject e : listLiterals){
					literals.add(e.toString());
				}
				EnumType et = TypeFactory.mkEnum(nameOfEnum, literals);
				
				try {
					newModel.addEnumType(et);
				} catch (MInvalidModelException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
				
		ArrayList<EReference> listEre = new ArrayList<>();
		for(EObject eo : listElement){
			if(eo.eClass().getName() == "EClass"){
				EClass temp = (EClass) eo;
				// Convert super type
				MClass child = newModel.getClass(temp.getName());
				// Get all super type
				EList<EClass> listSuperType = temp.getESuperTypes();
				if(listSuperType.size() > 0){
					for(EClass e : listSuperType){
						MClass parent = newModel.getClass(e.getName());
						MGeneralization mg = mf.createGeneralization(child, parent);
						try {
							newModel.addGeneralization(mg);
						} catch (MInvalidModelException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
				}
				
				// Convert reference
				EList<EReference> ereferen = temp.getEAllReferences();
				for(EReference e : ereferen){
					if(e.getEOpposite() == null) {
						System.out.println(e.toString());
					}
					listEre.add(e);
				}
			}
		}
		
		while(!listEre.isEmpty()){
			EReference temp = listEre.get(0);
			EReference tempOpposite = null;
			int index = 1;
			while(index < listEre.size()){
				if(temp.equals(listEre.get(index).getEOpposite())){
					tempOpposite = listEre.remove(index);
					break;
				}else{
					index++;
				}
			}
			
			if(tempOpposite != null) {
				// DO something
				MMultiplicity mm = mf.createMultiplicity();
				mm.addRange(temp.getLowerBound(), temp.getUpperBound());
				MMultiplicity mm2 = mf.createMultiplicity();
				mm2.addRange(tempOpposite.getLowerBound(), tempOpposite.getUpperBound());
				
				MAssociation ma = mf.createAssociation("");
				MClass referenceTo = null;
				MClass yourself = null;
				referenceTo = newModel.getClass(temp.getEContainingClass().getName());
				yourself = newModel.getClass(tempOpposite.getEContainingClass().getName());
//				for(MClass mc : allClass){
//					if(mc.name() == temp.getEContainingClass().getName()){
//						yourself = mc;
//					}
//					if(mc.name() == tempOpposite.getEContainingClass().getName()){
//						referenceTo = mc;
//					}
//				}
				
				MAssociationEnd mae = mf.createAssociationEnd(yourself, temp.getName(), mm2, MAggregationKind.NONE, temp.isOrdered(), Collections.emptyList());;
				MAssociationEnd mae2 = mf.createAssociationEnd(referenceTo, tempOpposite.getName(), mm, MAggregationKind.NONE, tempOpposite.isOrdered(), Collections.emptyList());
				try {
					ma.addAssociationEnd(mae);
					ma.addAssociationEnd(mae2);
				} catch (MInvalidModelException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				try {
					newModel.addAssociation(ma);
				} catch (MInvalidModelException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			if(tempOpposite == null) {
				System.out.println(temp.toString() + "false");
			}
			
			listEre.remove(0);
		}
		
		for(EObject x : listElement){
			// Convert all class
			if(x.eClass().getName() == "EClass"){
				EClass temp = (EClass) x;
				MClass cls = newModel.getClass(temp.getName());
				// Add constraint
				EList<EAnnotation> listAnnotation = temp.getEAnnotations();
				if(listAnnotation.size() > 0) {
					String[] listNameConstraint = listAnnotation.get(0).getDetails().get(0).getValue().split(" ");
					for (String string : listNameConstraint) {
						System.out.println(cls.name() + " " + string + "  " + listAnnotation.get(1).getDetails().get(string));
						UseModelApi useModelApi = new UseModelApi(newModel);
						try {
							newModel.addClassInvariant(useModelApi.createInvariant(string, cls.name(), listAnnotation.get(1).getDetails().get(string), false));
						} catch (MInvalidModelException | UseApiException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		}
		
		MSystem mySystem = new MSystem(newModel);
		mySession.setSystem(mySystem);
		
	}
}