package com.carneiro.mcredsim;
import java.awt.Color;
import java.awt.image.IndexColorModel;


public class Colors {
	public static final Color
		air = Color.WHITE,
		wireOn = Color.RED,
		wireOff = new Color(0x800000),
		block = Color.YELLOW,
		cover = new Color(0x80808080,true),
		fog = new Color(0x40FFFFFF,true),
		aircover = new Color(0x60FFFFFF,true),
		valve = Color.GRAY,
		button = new Color(0x4D4E50),
		door = new Color(0x614226),
		grid = Color.GRAY,
		hilite = new Color(0xA0B19C),
		copyFrom = new Color(0x3E88F9),
		copyTo = new Color(0xFB6612),
		dirt = new Color(0x856043),
		sand = new Color(0xDBD371),
		water = new Color(0x2A5EFF),
		tooltip = new Color(0xC0DDDDDD,true);
	public static final IndexColorModel icm;
	static {
		Color[] trans=new Color[]{fog,aircover,cover};
		Color[][] cols=new Color[][]{{new Color(0),
			 air,wireOn,wireOff,block,valve,button,door,dirt,sand,water},
			    {wireOn,wireOff,      valve,button,door},
			    {wireOn,wireOff},
			{air,wireOn,wireOff,block,      button,door}
		};
		int m=0,p=0;
		for (Color[] c:cols) m+=c.length;
		byte[] r=new byte[m],g=new byte[m], b=new byte[m];
		for (int j=0;j<cols[0].length;j++,p++) {
			r[p]=(byte)cols[0][j].getRed();
			g[p]=(byte)cols[0][j].getGreen();
			b[p]=(byte)cols[0][j].getBlue();
		}
		for (int i=1;i<cols.length;i++) {
			double a=trans[i-1].getAlpha()/255.0;
			for (int j=0;j<cols[i].length;j++,p++) {
				r[p]=(byte)(cols[i][j].getRed()  *(1-a)+trans[i-1].getRed()  *a-.01);
				g[p]=(byte)(cols[i][j].getGreen()*(1-a)+trans[i-1].getGreen()*a-.01);
				b[p]=(byte)(cols[i][j].getBlue() *(1-a)+trans[i-1].getBlue() *a-.01);
			}
		}
		int d=0, m2=m-1;
		while (m2>0) {m2>>=1; d++;}
		icm=new IndexColorModel(d, m, r, g, b, 0);
	}
}
