package com.lint.plus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

public class LintUnsedResources {
	public Map<String, HashSet<String>> deleteMapData = new HashMap<String, HashSet<String>>();
	private String messageRegex = "R\\.(\\w+)\\.(\\w+)";//R.string.name
	private File resourcesFile;
	private File unusedFile;
	private File lintBatFile;
	private xmlUtils xmlUtils;

	public LintUnsedResources(String resources) {
		resourcesFile = new File(resources);
		unusedFile = new File(resourcesFile, "unusedResources.xml");
		//lintBatFile = new File("F:\\android-sdk-windows\\tools\\lint.bat");
		lintBatFile = new File("F:\\Android\\Sdk\\tools\\bin\\lint.bat");
		xmlUtils = new xmlUtils();
	}

	public void run() {
		if (!resourcesFile.exists()) {
			System.err.println(String.format("%s no exists!", resourcesFile.getAbsolutePath()));
			return;
		}
		if (unusedFile.exists()) {
			unusedFile.delete();
		}
		runLintbat();
		if (!unusedFile.exists()) {
			System.err.println(String.format("%s no exists!", unusedFile.getAbsolutePath()));
			return;
		}
		analysisResultXml();
		deleteResources();
	}

	private void deleteResources() {
		for (String key : deleteMapData.keySet()) {
			//System.out.println(key);
			HashSet<String> hashSet = deleteMapData.get(key);
			/*for (String value : hashSet) {
				System.out.println(String.format("key = %s value = %s", key, value));
			}*/
			deleteXml(key, hashSet);
		}
	}

	private void deleteXml(String subFile, HashSet<String> hashSet) {
		File xmlFile = new File(resourcesFile, subFile);
		Document srcDocument = xmlUtils.read(xmlFile);
		if (srcDocument == null) {
			return;
		}
		Element rootElement = srcDocument.getRootElement();
		for (Iterator iterator = rootElement.elementIterator(); iterator.hasNext();) {
			Element element = (Element) iterator.next();
			String name = element.attributeValue("name");
			if (hashSet.contains(name)) {
				rootElement.remove(element);
				System.out.println(String.format("delete %s name : %s", subFile, name));
			}
		}
		xmlUtils.writeDocument(xmlFile, srcDocument);
	}

	public void analysisResultXml() {
		Document document = xmlUtils.read(unusedFile);
		if (document == null) {
			return;
		}
		Element issuesElements = document.getRootElement();

		for (Iterator issue = issuesElements.elementIterator(); issue.hasNext();) {
			Element issueElement = (Element) issue.next();
			String message = issueElement.attributeValue("message");
			System.out.println(message);
			final Matcher matcher = Pattern.compile(messageRegex).matcher(message);
			String type = null;
			String name = null;
			while (matcher.find()) {
				type = matcher.group(1);
				name = matcher.group(2);
				//System.out.println(String.format("type = %s name = %s", type, name));
			}
			for (Iterator location = issueElement.elementIterator("location"); location.hasNext();) {
				Element locationElement = (Element) location.next();
				String file = locationElement.attributeValue("file");
				//System.out.println(String.format("message = %s location : %s", message, file));
				String line = locationElement.attributeValue("line");
				String column = locationElement.attributeValue("column");
				if (line != null || column != null) {
					//System.out.println(String.format("line = %s column = %s ", line, column));
					putDeleteMapData(file, name);
				} else {
					File mDeleteFile = new File(resourcesFile, file);
					mDeleteFile.delete();
					System.out.println("delete " + file);
					//System.out.println("delete " + mDeleteFile.getAbsolutePath());
				}
			}

		}

	}

	public void putDeleteMapData(String key, String value) {
		if (deleteMapData.get(key) == null) {
			HashSet<String> data = new HashSet<String>();
			data.add(value);
			deleteMapData.put(key, data);
		} else {
			HashSet<String> data = deleteMapData.get(key);
			data.add(value);
			deleteMapData.put(key, data);
		}
	}

	private void runLintbat() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("cmd.exe /c start /b ");
		buffer.append(lintBatFile.getAbsolutePath());
		buffer.append(" ");
		buffer.append("--check ");
		buffer.append("\"UnusedResources\" ");
		buffer.append("--xml ");
		buffer.append(unusedFile.getAbsolutePath());
		buffer.append(" ");
		buffer.append(resourcesFile.getAbsolutePath());
		System.out.println(buffer.toString());

		try {
			Process process = Runtime.getRuntime().exec(buffer.toString());
			InputStreamReader inputStr = new InputStreamReader(process.getInputStream());
			BufferedReader br = new BufferedReader(inputStr);
			String temp = "";
			while ((temp = br.readLine()) != null) {
				System.out.println(temp);
				//lint.bat 文件中添加echo finish_Lint_Bat作为结束语句
				if (temp.contains("finish_Lint_Bat")) {
					break;
				}
			}
			process.destroy();
			br.close();
			inputStr.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
