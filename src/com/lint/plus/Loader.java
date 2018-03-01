package com.lint.plus;

import java.io.File;
import java.io.PrintStream;

import org.dom4j.Document;

public class Loader extends BaseCommand {

	public static void main(final String[] args) {
		(new Loader()).run(args);
	}

	@Override
	public void onRun() throws Exception {
		//log("mArgs :" + Arrays.toString(mArgs));
		//log("length = "+mArgs.length);
		String op = nextArgRequired();
		if (op.equals("cp") && mArgs.length >= 3) {
			runCopy();
		} else if (op.equals("del")) {
			runDelete();
		}else if (op.equals("miss")) {
			runMissingTranslation();
		}else if (op.equals("sort")) {
			runSort();
		} else {
			showError("Error: unknown command '" + op + "'");
		}
	}

	private void runCopy() {
		CopyResourceScanner resourceScanner = new CopyResourceScanner(nextArgRequired(), nextArgRequired());
		if (mArgs.length == 4 && mArgs[3] != null) {
			resourceScanner.setTargetFile(mArgs[3]);
		}
		resourceScanner.run();
	}

	private void runDelete() {
		String op = nextArgRequired();
		LintUnsedResources lintUnsedResources = new LintUnsedResources(op);
		lintUnsedResources.run();
		
	}
	private void runMissingTranslation() {
		String op = nextArgRequired();
		LintMissingTranslation lintMissingTranslation = new LintMissingTranslation(op);
		lintMissingTranslation.run();
		
	}

	private void runSort() {
		File referenceXmlFile = new File(nextArgRequired());
		File targetXmlFile = new File(nextArgRequired());
		if (referenceXmlFile.exists() && targetXmlFile.exists()) {
			xmlUtils xmlUtils = new xmlUtils();
			Document document = xmlUtils.sortXml(referenceXmlFile, targetXmlFile, true);
			if (document != null)
				xmlUtils.writeDocument(targetXmlFile, document);
		}
		log("runSort finish");
	}

	/* 
	 * cp resourcesFile(资源文件) searchFile（分析的文件） targetFile（目标文件）
	 * del G:\Android\test\Launcher
	 */
	@Override
	public void onShowUsage(PrintStream out) {
		out.print(" cp resourcesFile searchFile targetFile\n del resourcesFile\n miss resourcesFile" +
				"\n sort referenceXmlFile targetXmlFile");
	}

}
