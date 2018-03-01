package dev.marianoalipi.simulador;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;

public class Tools {

	public static Font makeFont(String fontFace, String style, int size) {
		int intStyle = Font.PLAIN;
		if (style == "PLAIN") intStyle = Font.PLAIN;
		else if (style == "BOLD") intStyle = Font.BOLD;
		else if (style == "ITALICS") intStyle = Font.ITALIC;
		Font font = new Font(fontFace, intStyle, size);
		return font;
	}
	public static GridBagConstraints makeC(int gridx, int gridy) {
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = gridx;
		c.gridy = gridy+1;
		c.insets = new Insets(5,5,5,5); // Default insets.
		return c;
	}
	public static GridBagConstraints makeC(int gridx, int gridy, int gridwidth, int gridheight) {
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = gridx;
		c.gridy = gridy+1;
		c.gridwidth = gridwidth;
		c.gridheight = gridheight;
		c.insets = new Insets(5,5,5,5); // Default insets.
		return c;
	}
	public static GridBagConstraints makeC(int gridx, int gridy, int gridwidth, int gridheight, int anchor, int fill) {
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = gridx;
		c.gridy = gridy+1;
		c.gridwidth = gridwidth;
		c.gridheight = gridheight;
		c.anchor = anchor;
		c.fill = fill;
		c.insets = new Insets(5,5,5,5); // Default insets.
		return c;
	}
	public static GridBagConstraints makeC(int gridx, int gridy, int gridwidth, int gridheight, double weightx, double weighty, int anchor, int fill) {
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = gridx;
		c.gridy = gridy+1;
		c.gridwidth = gridwidth;
		c.gridheight = gridheight;
		c.weightx = weightx;
		c.weighty = weighty;
		c.anchor = anchor;
		c.fill = fill;
		c.insets = new Insets(5,5,5,5); // Default insets.
		return c;
	}
	
}
