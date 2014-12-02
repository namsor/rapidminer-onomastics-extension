package com.namsor.api.rapidminer;

import java.awt.Desktop;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import com.rapidminer.gui.wizards.PreviewCreator;
import com.rapidminer.gui.wizards.PreviewListener;
import com.sun.org.apache.xerces.internal.util.URI;


public class OriginAPIPreviewCreator implements PreviewCreator {

	/**
	 * Serial UID
	 */
	private static final long serialVersionUID = -3234041420858447980L;

	private final static String API_TITLE = "Getting an API Key";
	
	private final static String API_MSG = "Get an API Key on "+ExtractOriginOperator.MASHAPE_CHANNEL_USER+" then set "+
			ExtractOriginOperator.API_CHANNEL_SECRET+"=<your api key> and "+
			ExtractOriginOperator.API_CHANNEL_USER+"="+ExtractOriginOperator.MASHAPE_CHANNEL_USER;
	
	public OriginAPIPreviewCreator() {
	}

	public void createPreview(PreviewListener listener) {
		try {
			JOptionPane.showMessageDialog(new JFrame(), API_MSG, API_TITLE, JOptionPane.INFORMATION_MESSAGE);
			Logger.getLogger(getClass().getName()).log(
					Level.INFO,
					"Please get your API Key at "+ExtractOriginOperator.MASHAPE_CHANNEL_REGISTRATION_URL);		
			
			URL url = new URL(ExtractOriginOperator.MASHAPE_CHANNEL_REGISTRATION_URL);
			// propagate click to Operator
			listener.getProcess();
			openWebpage(url);
		} catch (MalformedURLException e) {
			Logger.getLogger(getClass().getName()).log(
					Level.WARNING,
					"Couldn't open in browser. Please get your API Key at "+ExtractOriginOperator.MASHAPE_CHANNEL_REGISTRATION_URL);		
		}
	}

	public static void openWebpage(java.net.URI uri) {
	    Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
	    if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
	        try {
	            desktop.browse(uri);
	        } catch (Exception e) {
				Logger.getLogger(OriginAPIPreviewCreator.class.getName()).log(
						Level.WARNING,
						"Couldn't open in browser. Please get your API Key at "+ExtractOriginOperator.MASHAPE_CHANNEL_REGISTRATION_URL);		
	        }
	    }
	}

	public static void openWebpage(URL url) {
	    try {
	        openWebpage(url.toURI());
	    } catch (URISyntaxException e) {
			Logger.getLogger(OriginAPIPreviewCreator.class.getName()).log(
					Level.WARNING,
					"Couldn't open in browser. Please get your API Key at "+ExtractOriginOperator.MASHAPE_CHANNEL_REGISTRATION_URL);		
	    }
	}
}
