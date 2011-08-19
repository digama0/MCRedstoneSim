package com.carneiro.mcredsim;
import static java.awt.event.KeyEvent.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;

import javax.imageio.*;
import javax.imageio.metadata.*;
import javax.imageio.stream.ImageOutputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.filechooser.FileFilter;

import org.w3c.dom.Node;


public class Viewport implements MouseWheelListener {
	public static final String title="Redstone Simulator v2.1";
	static final Object[][] blockNames = new Object[][]{
		{"Rock",1}, {"Grass/Dirt",2}, {"Dirt",3}, {"Cobblestone",4},
		{"Wood",5}, {"Adminium",7}, {"Sand",12}, {"Gravel",13},
		{"Gold ore",14}, {"Iron ore",15}, {"Coal ore",16}, {"Tree trunk",17},
		{"Sponge",19}, {"Cloth",35}, {"Gold",41}, {"Iron",42},
		{"Double stair",43}, {"Brick",45}, {"Bookshelf",47},
		{"Mossy cobblestone",48}, {"Obsidian",49}, {"Diamond ore",56},
		{"Diamond",57}, {"Redstone ore",73}, {"Snow",80}, {"Clay",82}
	};
	static final int iX=30, iY=20, iZ=7, MS_PER_TICK=62, GIF_DELAY=200 /* hundredths */;
	static final boolean IS_MAC=
		System.getProperty("os.name").toLowerCase().indexOf("mac")!=-1;
	static final boolean doubleDoors=false;
	static boolean waterMode=false;
	int block=2;
	JFrame frame;
	JDialog adjF, optF;
	JPanel view, pView;
	StatusBar stats;
	Field field;
	Palette[] palArr=Palette.pal3;
	int pal, scale=3, pScale=5, lyr=0, gd=0,
		x, y, z, cloneMode=0, lastX, lastY, lastPX, lastPY;
	int[] clone=null;
	JLabel lLyr, lSz, lLoc, lRed, lTorch, lTot, tooltip;
	JSeparator sLoc;
	JButton tick, play;
	JCheckBox c1Lyr, c3Lyr, cBridge, cWater;
	Icon playII, pauseII;
	JMenuItem tickMI;
	File save=null, folder=new File("~");
	boolean playing=false, isCtrlDown=false, modified=false;
	Timer pTimer;

	private Icon getImage(String loc, int s, String desc)
	{
		URL u=getClass().getResource("/images/"+loc+s+".gif");
		if (u==null)
			return new MissingIcon(s, s);
		else return new ImageIcon(u, desc);
	}

	private URL getSound(String loc)
	{
		return getClass().getResource("/sound/"+loc);
	}
	private void addCtrl(JPanel p, JComponent c)
	{
		c.setAlignmentX(Component.CENTER_ALIGNMENT);
		p.add(c);
	}
	private JButton addBtn(JPanel p, String img, String t, ActionListener a)
	{
		JButton b=new JButton(t);
		b.setMargin(new Insets(0,0,0,0));
		b.setPreferredSize(new Dimension(40,20));
		b.setMaximumSize(new Dimension(40,20));
		//b.setIcon(getImage(img,16,img));
		b.addActionListener(a);
		addCtrl(p,b);
		return b;
	}
	private void addAdjCol(JPanel p, String s, String b1, String b2, boolean e1, boolean e2, int e3)
	{
		JPanel p2 = new JPanel();
		p2.setLayout(new BoxLayout(p2, BoxLayout.PAGE_AXIS));
		JLabel l=new JLabel(s);
		l.setAlignmentX(Component.CENTER_ALIGNMENT);
		Dimension d=l.getPreferredSize();
		l.setMaximumSize(new Dimension(d.width+1,d.height));
		p2.add(l);
		p2.add(Box.createRigidArea(new Dimension(5,5)));
		addBtn(p2,(e1?"Grow":"Shrink")+s,b1,new Expander(this,e1,e2,e3));
		p2.add(Box.createRigidArea(new Dimension(5,5)));
		addBtn(p2,(e1?"Shrink":"Grow")+s,b2,new Expander(this,!e1,e2,e3));
		p.add(p2);
		p.add(Box.createRigidArea(new Dimension(5,5)));
	}
	public void setSize(int _x, int _y, int _z)
	{
		x=_x; y=_y; z=_z;
		lSz.setText(x+"x"+y+"x"+z);
		Dimension d=new Dimension((x*9+1)*scale,(y*9+1)*scale);
		view.setPreferredSize(d);
		view.revalidate();
	}
	public void addScale(int ds)
	{
		scale+=ds;
		if (scale<1) scale=1;
		Dimension d=new Dimension((x*9+1)*scale,(y*9+1)*scale);
		view.setPreferredSize(d);
		view.revalidate();
		view.repaint();
	}
	public void setLyr(int l)
	{
		if (l<0) l=0;
		if (l>=z) l=z-1;
		lyr=l;
		lLyr.setText("Layer "+(lyr+1));
		if (lyr<gd)
			lLyr.setForeground(Colors.dirt);
		else
			lLyr.setForeground(Color.BLUE.darker());
		if (clone!=null)
			if (clone.length==6)
				setClone(clone[3],clone[4],lyr);
			else if (clone.length==9)
				setClone(clone[6],clone[7],lyr);
	}
	public void recountRed()
	{
		field.wires=field.torches=0;
		for (int i=0;i<z;i++)
			for (int j=0;j<y;j++)
				for (int k=0;k<x;k++)
					if (field.g(k,j,i)==Blocks.wire)
						field.wires++;
					else if (field.g(k,j,i)==Blocks.torch)
						field.torches++;
		updateRed();
	}
	public void updateRed()
	{
		lRed.setText(field.wires+"");
		lTorch.setText(field.torches+"");
		lTot.setText((field.wires+field.torches)+"");
		stats.revalidate();
	}
	private int[] findDoor(int x, int y, int z)
	{
		for (int z2=z-1;z2<z+2;z2++)
		{
			if (field.g(x-1,y,z2)==Blocks.doorA) return new int[]{x-1,y,z2,1};
			if (field.g(x+1,y,z2)==Blocks.doorA) return new int[]{x+1,y,z2,2};
			if (field.g(x,y-1,z2)==Blocks.doorA) return new int[]{x,y-1,z2,3};
			if (field.g(x,y+1,z2)==Blocks.doorA) return new int[]{x,y+1,z2,4};
		}
		return null;
	}
	private void place(int x, int y, int z, Palette pal)
	{
		if (field.match(x,y,z,pal) && pal!=Palette.water) return;
		if (pal==Palette.door && z==this.z-1) return;
		if (pal.a.wall==3 && !field.g(x,y+1,z).block()
		                  && !field.g(x,y-1,z).block()
		                  && !field.g(x+1,y,z).block()
		                  && !field.g(x-1,y,z).block())
			return;
		modify();
		field.s(x,y,z,pal.a);
		if (Field.layers>1)
		{
			if (pal.b!=null)
				field.s(x,y,z+1,pal.b);
			if (Field.layers>2 && pal.c!=null)
					field.s(x,y,z+2,pal.c);
		}
		if (pal.a==Blocks.wire || pal.a==Blocks.press)
			field.s(x,y,z-1,Blocks.block);
		int[] d;
		if (pal==Palette.water)
		{
			field.sp(x,y,z,16);
		}
		else if (pal==Palette.door &&
			(d=findDoor(x,y,z))!=null &&
			field.w(d[0],d[1],d[2])==d[3])
			if (doubleDoors)
			{
				field.s(x,y,z,field.w(d[0],d[1],d[2]));
				field.s(x,y,z+1,2);
			}
			else
			{
				field.s(x,y,z,4-field.w(d[0],d[1],d[2]));
				field.s(x,y,z+1,1);
			}
		else
		{
			int p=z+1;
			if (field.g(x,y,z).wall!=0) p--;
			if (p==z || field.g(x,y,z+1).wall!=0)
			{
				int w=0;
				if (field.g(x,y,p)==Blocks.torch)
					field.sp(x,y,p,16);
				do w=++w%5;
				while (!field.s(x,y,p,w));
				if (field.w(x,y,p)==0 || field.g(x,y,z).wall==2)
					field.s(x,y,p-1,Blocks.block);
				if (pal==Palette.door)
					field.s(x,y,p+1,1);
			}
		}
		field.update();
		view.repaint();
	}
	public Viewport(int _x, int _y, int _z)
	{
		x=_x; y=_y; z=_z;
		field = new Field(this,x,y,z);
		init();
	}
	public Viewport(byte[][][] d, byte[][][] e)
	{
		x=d[0][0].length;
		y=d[0].length;
		z=d.length;
		field = new Field(this,d,e);
		init();
		recountRed();
		field.update();
	}
	private void init()
	{
		init_setupFrame();
		init_buildAdjust();
		init_buildOptions();
		frame.setContentPane(
			init_buildMenuBar(
				init_buildStatusBar(
					init_buildToolbar(
						init_buildPView(
							init_buildView())))));
		init_doKeyBindings();
		setSize(x, y, z);
		frame.pack();
		frame.setLocationByPlatform(true);
		view.requestFocusInWindow();
		frame.setVisible(true);
	}
	private void init_setupFrame()
	{
		for (int i=0;i<palArr.length;i++)
			if (palArr[i]==Palette.wire)
				pal = i;
		frame = new JFrame(title); 
		URL u=getClass().getResource("/images/Logo16.png");
		if (u!=null)
		{
			Image im=new ImageIcon(u, "").getImage();
			ArrayList<Image> al=new ArrayList<Image>();
			al.add(im);
			for (int i=16;i<256;i*=2)
				al.add(im.getScaledInstance(i, i, Image.SCALE_REPLICATE));
			frame.setIconImages(al);
		}
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				new ButtonAction(ButtonAction.EXIT).actionPerformed(null);
			}
		});
		//frame.setResizable(false);
	}
	private void init_buildAdjust()
	{
		adjF = new JDialog(frame,"Adjust Size");
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.LINE_AXIS));
		p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 0));

		addAdjCol(p, "Top",    "\u2191", "\u2193", true, false, 1);
		addAdjCol(p, "Bottom", "\u2191", "\u2193", false, true, 1);
		addAdjCol(p, "Left",   "\u2192", "\u2190", false,false, 2);
		addAdjCol(p, "Right",  "\u2192", "\u2190", true,  true, 2);
		addAdjCol(p, "Front",  "\u2299", "\u2295", true,  true, 0);
		addAdjCol(p, "Back",   "\u2299", "\u2295", false,false, 0);

		adjF.setContentPane(p);
		adjF.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		//adjF.setResizable(false);
		adjF.pack();
		adjF.setLocationByPlatform(true);
	}
	private void init_buildOptions()
	{
		optF = new JDialog(frame,"Options");
		JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.PAGE_AXIS));
		p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		JCheckBox c;
		p.add(c=new JCheckBox("Cyclic (in X and Z only) "));
		c.addItemListener(new OptionAction(OptionAction.CYCLIC));
		p.add(c=new JCheckBox("\"Natural\" wire connections "));
		c.addItemListener(new OptionAction(OptionAction.NEW_WIRE));
		p.add(c=new JCheckBox("Ground switches power blocks below "));
		c.setSelected(true);
		c.addItemListener(new OptionAction(OptionAction.DUMMY_SW));
		p.add(c1Lyr=new JCheckBox("Show only one layer "));
		c1Lyr.addItemListener(new OptionAction(OptionAction.LAYER1));
		p.add(c3Lyr=new JCheckBox("Show three layers "));
		c3Lyr.setSelected(true);
		c3Lyr.addItemListener(new OptionAction(OptionAction.LAYER3));
		p.add(cBridge=new JCheckBox("Show bridges "));
		cBridge.setSelected(true);
		cBridge.addItemListener(new OptionAction(OptionAction.BRIDGE));
		JComboBox cb;
		String[] bS=new String[blockNames.length];
		for (int i=0;i<bS.length;i++) bS[i]=(String)blockNames[i][0];
		p.add(cb=new JComboBox(bS));
		cb.setAlignmentX(0);
		cb.setSelectedIndex(1);
		cb.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				block=(Integer)blockNames[((JComboBox)
					e.getSource()).getSelectedIndex()][1];
			}
		});
		cWater=new JCheckBox("Water circuit mode ");
//		p.add(cWater);
		cWater.addItemListener(new OptionAction(OptionAction.WATER));

		//SpringUtilities.makeCompactGrid(p, 7, 1, 5, 5, 5, 5);

		optF.setContentPane(p);
		optF.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
		optF.setResizable(false);
		optF.pack();
		optF.setLocationByPlatform(true);
	}
	private JScrollPane init_buildView()
	{
		view = new JPanel(null) {
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				paintView(g);
			}
		};
		view.addMouseMotionListener(new MouseMotionListener() {
			public void mouseMoved(MouseEvent e) {viewMouseMoved(e, false);}
			public void mouseDragged(MouseEvent e) {viewMouseMoved(e, true);}
		});
		view.addMouseListener(new MouseListener() {
			public void mouseReleased(MouseEvent e) {}
			public void mousePressed(MouseEvent e) {viewMousePressed(e);}
			public void mouseExited(MouseEvent e) {lastPX=lastPY=-1; updateTooltip();}
			public void mouseEntered(MouseEvent e) {}
			public void mouseClicked(MouseEvent e) {}
		});

		frame.addComponentListener(new ComponentListener() {
			public void componentShown(ComponentEvent e) {}
			public void componentMoved(ComponentEvent e) {}
			public void componentHidden(ComponentEvent e) {}
			public void componentResized(ComponentEvent e) {frameResized();}
		});
		view.add(tooltip=new JLabel(""));
		{
			tooltip.setVisible(false);
			tooltip.setOpaque(true);
			tooltip.setBackground(Colors.tooltip);
		}
		JScrollPane sp=new JScrollPane(view);
		view.setFocusable(true);
		view.setFocusTraversalKeysEnabled(false);

		sp.setAlignmentX(Component.CENTER_ALIGNMENT);
		sp.setWheelScrollingEnabled(false);
		frame.addMouseWheelListener(this);
		sp.addMouseWheelListener(this);
		return sp;
	}
	private void init_doKeyBindings()
	{
		InputMap im=view.getInputMap(JComponent.WHEN_FOCUSED);
		//im.put(KeyStroke.getKeyStroke("W"), "up");
		im.put(KeyStroke.getKeyStroke("UP"), "up");
		//im.put(KeyStroke.getKeyStroke("S"), "down");
		im.put(KeyStroke.getKeyStroke("DOWN"), "down");
		im.put(KeyStroke.getKeyStroke("PERIOD"), "tick");
		im.put(KeyStroke.getKeyStroke("ctrl PERIOD"), "tick");
		im.put(KeyStroke.getKeyStroke("ENTER"), "pause");
		im.put(KeyStroke.getKeyStroke("ctrl ENTER"), "pause");
		im.put(KeyStroke.getKeyStroke("ESCAPE"), "esc");
		im.put(KeyStroke.getKeyStroke("CONTROL"), "cDown");
		im.put(KeyStroke.getKeyStroke("released CONTROL"), "cUp");
		view.getActionMap().put("up",new ButtonAction(ButtonAction.LYR_UP));
		view.getActionMap().put("down",new ButtonAction(ButtonAction.LYR_DN));
		view.getActionMap().put("tick",new ButtonAction(ButtonAction.TICK));
		view.getActionMap().put("pause",new ButtonAction(ButtonAction.PLAYPAUSE));
		view.getActionMap().put("esc",new ButtonAction(ButtonAction.CLONE_ESC));
		view.getActionMap().put("cDown",new AbstractAction() {
			public void actionPerformed(ActionEvent e) {isCtrlDown=true; updateTooltip(); System.out.println("down");}});
		view.getActionMap().put("cUp",new AbstractAction() {
			public void actionPerformed(ActionEvent e) {isCtrlDown=false; updateTooltip();}});
	}
	private JPanel init_buildPView(JScrollPane sp)
	{
		pView = new JPanel() {
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				paintPView(g);
			}
		};
		Dimension d=new Dimension((palArr.length*9+1)*pScale,10*pScale);
		pView.setPreferredSize(d);
		pView.addMouseListener(new MouseListener() {
			public void mouseReleased(MouseEvent e) {}
			public void mousePressed(MouseEvent e) {pViewMousePressed(e);}
			public void mouseExited(MouseEvent e) {}
			public void mouseEntered(MouseEvent e) {}
			public void mouseClicked(MouseEvent e) {}
		});

		JPanel main=new JPanel();
		GroupLayout layout = new GroupLayout(main);
		main.setLayout(layout);
		
		pView.setAlignmentX(Component.CENTER_ALIGNMENT);
		JLabel lC=new JLabel(),lD=new JLabel();
		layout.setHorizontalGroup(
			layout.createParallelGroup()
				//.addGroup(layout.createSequentialGroup()
				//	.addComponent(lA, 0, 0, Short.MAX_VALUE)
				//	.addComponent(tools, 0, -1, -1) // -1 = default size
				//	.addComponent(lB, 0, 0, Short.MAX_VALUE))
				.addComponent(sp, 0, -2, Short.MAX_VALUE)
				.addGroup(layout.createSequentialGroup()
					.addComponent(lC, 0, 0, Short.MAX_VALUE)
					.addComponent(pView, -2, -2, -2) // -2 = preferred size
					.addComponent(lD, 0, 0, Short.MAX_VALUE)));
		layout.setVerticalGroup(
			layout.createSequentialGroup()
				//.addGroup(layout.createParallelGroup()
				//	.addComponent(lA, 0, 0, 0)
				//	.addComponent(tools, -1, -1, -1)
				//	.addComponent(lB, 0, 0, 0))
				.addComponent(sp, 0, -2, Short.MAX_VALUE)
				.addGroup(layout.createParallelGroup()
					.addComponent(lC, 0, 0, 0)
					.addComponent(pView, -2, -2, -2)
					.addComponent(lD, 0, 0, 0)));
		return main;
	}
	private JPanel init_buildToolbar(JPanel main)
	{
//		tools = new JPanel(new SpringLayout());
//
//		addCtrl(tools, new JLabel(""));
//		addCtrl(tools, new JLabel("File"));
//		addCtrl(tools, new JLabel("Time Control"));
//		addCtrl(tools, lLyr = new JLabel("Layer 1"));
//		lLyr.setForeground(Color.BLUE.darker());
//		addCtrl(tools, lSz = new JLabel("("+x+"x"+y+"x"+z+")"));
//		addCtrl(tools, lRed = new JLabel("0 Redstone"));
//
//		addBtn(tools, false, "New", new ButtonAction(ButtonAction.NEW));
//		addBtn(tools, false, "Save", new ButtonAction(ButtonAction.SAVE));
//		tick = addBtn(tools, false, "Tick", new ButtonAction(ButtonAction.TICK));
//		addBtn(tools, true, "\u2191", new ButtonAction(ButtonAction.LYR_UP));
//		addBtn(tools, false, "Adjust...", new ButtonAction(ButtonAction.ADJUST))
//			.setMinimumSize(new Dimension(75, 20));
//		addCtrl(tools,lTorch=new JLabel("0 Torches"));
//
//		addBtn(tools, false, "Clone", new ButtonAction(ButtonAction.CLONE));
//		addBtn(tools, false, "Open", new ButtonAction(ButtonAction.OPEN));
//		play = addBtn(tools, false, "Play", new ButtonAction(
//			ButtonAction.PLAYPAUSE));
//		addBtn(tools, true, "\u2193", new ButtonAction(ButtonAction.LYR_DN));
//		addBtn(tools, false, "Options...", new ButtonAction(ButtonAction.OPT))
//			.setMinimumSize(new Dimension(75, 20));
//		addCtrl(tools,lTot=new JLabel("0 total"));
//
//		lRed.setForeground(Colors.wireOn);
//		lTorch.setForeground(Colors.wireOn);
//		lTot.setForeground(Colors.wireOn);
//
//		SpringUtilities.makeCompactGrid(tools, 3, 6, 5, 5, 5, 5);

		JPanel p = new JPanel(new BorderLayout());
		JToolBar tb = new JToolBar("Tools");
		//tb.setMargin(new Insets(3,-2,0,3));
		tb.setRollover(true);
		addButton(tb,"New","New",ButtonAction.NEW);
		addButton(tb,"Open","Open",ButtonAction.OPEN);
		addButton(tb,"Save","Save",ButtonAction.SAVE);
		tb.addSeparator();
		addButton(tb,"Export as GIF","Camera",ButtonAction.GIF);
		addButton(tb,"Clone","Copy",ButtonAction.CLONE);
		tb.addSeparator();
		tick = addButton(tb,"Tick","StepForward",ButtonAction.TICK);
		play = addButton(tb,"Play","Play",ButtonAction.PLAYPAUSE);
		playII = play.getIcon();
		pauseII = getImage("Pause",24,"Pause");
		tb.addSeparator();
		addButton(tb,"Layer Up","Up",ButtonAction.LYR_UP);
		addButton(tb,"Layer Down","Down",ButtonAction.LYR_DN);
		tb.addSeparator();
		addButton(tb,"Zoom In","ZoomIn",ButtonAction.ZOOM_IN);
		addButton(tb,"Zoom Out","ZoomOut",ButtonAction.ZOOM_OUT);
		tb.addSeparator();
		addButton(tb,"Adjust Size","AlignCenter",ButtonAction.ADJUST);
		addButton(tb,"Options","Preferences",ButtonAction.OPT);
		//System.out.println(tb.getMargin());
		p.add(main, BorderLayout.CENTER);
		p.add(tb, BorderLayout.PAGE_START);
		return p;
	}
	private JButton addButton(JToolBar tb, String name, String file, int action) {
		JButton b = new JButton();
		b.setToolTipText(name);
		b.addActionListener(new ButtonAction(action));
		b.setIcon(getImage(file,24,name));
		b.setFocusable(false);
		b.setFocusTraversalKeysEnabled(false);
		tb.add(b);
		return b;
	}

	private JPanel init_buildStatusBar(JPanel p)
	{
		JPanel p2 = new JPanel(new BorderLayout());
		p2.add(p, BorderLayout.CENTER);

		stats = new StatusBar();
		stats.add(lLyr = new JLabel("Layer 1"));
		lLyr.setForeground(Color.BLUE.darker());
		stats.addSeparator();
		stats.add(lSz = new JLabel(x+"x"+y+"x"+z));
		stats.addSeparator();
		stats.add(lLoc = new JLabel());
		sLoc = stats.addSeparator();
		lLoc.setVisible(false);
		sLoc.setVisible(false);
		stats.addGlue();
		stats.addSeparator();
		stats.add(lRed = new JLabel("0"));
		stats.add(new JLabel(getImage("Redstone",16,"Redstone")));
		stats.addSeparator();
		stats.add(lTorch = new JLabel("0"));
		stats.add(new JLabel(getImage("Torch",16,"Torches")));
		stats.addSeparator();
		stats.add(lTot = new JLabel("0"));
		stats.add(new JLabel(getImage("Ore",16,"Total")));
		p2.add(stats, BorderLayout.PAGE_END);

		lRed.setForeground(Colors.wireOff.darker());
		lTorch.setForeground(Colors.wireOff.darker());
		lTot.setForeground(Colors.wireOff.darker());
		return p2;
	}
	private JPanel init_buildMenuBar(JPanel p2)
	{
		JMenuBar menubar = new JMenuBar();
		int CTRL=Toolkit.getDefaultToolkit().getMenuShortcutKeyMask(),
			SHIFT=InputEvent.SHIFT_DOWN_MASK,
			ALT=InputEvent.ALT_DOWN_MASK;
		JMenu menu = new JMenu(" File "), menu2;
		{
			menu.setMnemonic(VK_F);
			menubar.add(menu);
			addMenuItem(menu, "New", VK_N, "New",
				CTRL, VK_N, new ButtonAction(ButtonAction.NEW));
			addMenuItem(menu, "Open", VK_O, "Open",
				CTRL, VK_O, new ButtonAction(ButtonAction.OPEN));
			menu.addSeparator();
			addMenuItem(menu, "Save", VK_S, "Save",
				CTRL, VK_S, new ButtonAction(ButtonAction.SAVE));
			addMenuItem(menu, "Save As...", VK_A, "SaveAs",
				CTRL | SHIFT, VK_S, new ButtonAction(ButtonAction.SAVEAS));
			menu.addSeparator();
			addMenuItem(menu, "Export as GIF", VK_G, "Camera",
				CTRL, VK_G, new ButtonAction(ButtonAction.GIF));
			menu.addSeparator();
			if (IS_MAC) addMenuItem(menu, "Quit", VK_Q, "Stop",
				CTRL, VK_Q, new ButtonAction(ButtonAction.EXIT));
			else addMenuItem(menu, "Exit", VK_X, "Stop",
				ALT, VK_F4, new ButtonAction(ButtonAction.EXIT));
		}
		menu = new JMenu(" Edit ");
		{
			menu.setMnemonic(VK_E);
			menubar.add(menu);
			addMenuItem(menu, "Clone", VK_C, "Copy",
				CTRL, VK_C, new ButtonAction(ButtonAction.CLONE));
			menu.addSeparator();
			tickMI=addMenuItem(menu, "Tick", VK_T, "StepForward",
				0, VK_PERIOD, new ButtonAction(ButtonAction.TICK));
			addMenuItem(menu, "Play/Pause", VK_P, "Play",
				0, VK_ENTER, new ButtonAction(ButtonAction.PLAYPAUSE));
		}
		menu = new JMenu(" View ");
		{
			menu.setMnemonic(VK_V);
			menubar.add(menu);
			addMenuItem(menu, "Layer Up", VK_U, "Up",
				0, VK_W, new ButtonAction(ButtonAction.LYR_UP));
			addMenuItem(menu, "Layer Down", VK_D, "Down",
				0, VK_S, new ButtonAction(ButtonAction.LYR_DN));
			menu.addSeparator();
			addMenuItem(menu, "Zoom In", VK_I, "ZoomIn",
				0, VK_EQUALS, new ButtonAction(ButtonAction.ZOOM_IN));
			addMenuItem(menu, "Zoom Out", VK_O, "ZoomOut",
				0, VK_MINUS, new ButtonAction(ButtonAction.ZOOM_OUT));
			menu.addSeparator();
			addMenuItem(menu, "Options...", VK_P, "Preferences",
				CTRL, VK_COMMA, new ButtonAction(ButtonAction.OPT));
			menu.addSeparator();
			
			menu2 = new JMenu("Look & Feel");
			{
				menu2.setMnemonic(VK_L);
				menu.add(menu2);
				ButtonGroup group = new ButtonGroup();
				String mn="";
				for (LookAndFeelInfo l:UIManager.getInstalledLookAndFeels())
				{
					JRadioButtonMenuItem i=new JRadioButtonMenuItem(l.getName(),
						l.getClassName().equals(UIManager.getCrossPlatformLookAndFeelClassName()));
					for (int j=0;j<l.getName().length();j++)
					{
						char c=l.getName().toLowerCase().charAt(j);
						if (!mn.contains(c+""))
						{
							mn+=c;
							i.setMnemonic(c);
							break;
						}
					}
					i.addActionListener(new LAFActionListener(l.getClassName()));
					group.add(menu2.add(i));
				}
			}
		}
		menu = new JMenu(" Adjust Size ");
		{
			menu.setMnemonic(VK_A);
			menubar.add(menu);
			addMenuItem(menu, "Adjust Size...", VK_A, "AlignCenter",
				CTRL, VK_A, new ButtonAction(ButtonAction.ADJUST));
			menu.addSeparator();
			menu2 = new JMenu("Shrink");
			{
				menu2.setMnemonic(VK_S);
				menu.add(menu2);
				addMenuItem2(menu2, "Top face down", VK_T,
					"ShrinkTop",   CTRL, VK_NUMPAD8, new Expander(this,false,false,1));
				addMenuItem2(menu2, "Bottom face up", VK_B,
					"ShrinkBottom",CTRL, VK_NUMPAD2, new Expander(this,false,true, 1));
				addMenuItem2(menu2, "Left face right", VK_L,
					"ShrinkLeft",  CTRL, VK_NUMPAD4, new Expander(this,false,false,2));
				addMenuItem2(menu2, "Right face left", VK_R,
					"ShrinkRight", CTRL, VK_NUMPAD6, new Expander(this,false,true, 2));
				addMenuItem2(menu2, "Front (sky) face down", VK_S,
					"ShrinkFront", CTRL, VK_NUMPAD9, new Expander(this,false,true, 0));
				addMenuItem2(menu2, "Back (ground) face up", VK_G,
					"ShrinkBack",  CTRL, VK_NUMPAD3, new Expander(this,false,false,0));
			}
			menu2 = new JMenu("Grow");
			{
				menu2.setMnemonic(VK_G);
				menu.add(menu2);
				addMenuItem2(menu2, "Top face up", VK_T,
					"GrowTop",   0, VK_NUMPAD8, new Expander(this,true,false,1));
				addMenuItem2(menu2, "Bottom face down", VK_B,
					"GrowBottom",0, VK_NUMPAD2, new Expander(this,true,true, 1));
				addMenuItem2(menu2, "Left face left", VK_L,
					"GrowLeft",  0, VK_NUMPAD4, new Expander(this,true,false,2));
				addMenuItem2(menu2, "Right face right", VK_R,
					"GrowRight", 0, VK_NUMPAD6, new Expander(this,true,true, 2));
				addMenuItem2(menu2, "Front (sky) face up", VK_S,
					"GrowFront", 0, VK_NUMPAD9, new Expander(this,true,true, 0));
				addMenuItem2(menu2, "Back (ground) face down", VK_G,
					"GrowBack",  0, VK_NUMPAD3, new Expander(this,true,false,0));
			}
		}
		p2.add(menubar,BorderLayout.PAGE_START);
		return p2;
	}
	private JMenuItem addMenuItem2(JMenu m, String name, int mnem, String file, int mod, int key, Action al) {
		JMenuItem i=addMenuItem(m, name, mnem, file, mod, key, al);
		view.getInputMap(JComponent.WHEN_FOCUSED).put(
			KeyStroke.getKeyStroke(key, mod | InputEvent.SHIFT_DOWN_MASK), file);
		view.getActionMap().put(file,al);
		return i;
	}
	private JMenuItem addMenuItem(JMenu m, String name, int mnem, String file, int mod, int key, ActionListener al) {
		JMenuItem i=new JMenuItem(name, mnem);
		if (file!=null)
			i.setIcon(getImage(file,16,name));
		if (key!=0)
			i.setAccelerator(KeyStroke.getKeyStroke(key,mod));
		i.addActionListener(al);
		return m.add(i);
	}

	private void paintView(Graphics g)
	{
		g.setColor(Colors.air);
		Rectangle r = g.getClipBounds();
		g.fillRect(r.x, r.y, r.width, r.height);
		Graphics2D g2=((Graphics2D)g.create());
		g2.scale(scale, scale);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(Colors.grid);
		g2.fillRect(0, 0, x*9+1, y*9+1);
		if (clone!=null)
		{
			int x1=clone[0],y1=clone[1],z1=clone[2],
				x2=clone[3],y2=clone[4],z2=clone[5];
			if (x2<x1) {x1=x2; x2=clone[0];}
			if (y2<y1) {y1=y2; y2=clone[1];}
			if (z2<z1) {z1=z2; z2=clone[2];}
			if (lyr>=z1 && lyr<=z2)
			{
				g2.setColor(Colors.copyFrom);
				g2.fillRect(x1*9, y1*9, (x2-x1)*9+10, 1);
				g2.fillRect(x1*9, y1*9, 1, (y2-y1)*9+10);
				g2.fillRect(x1*9, y2*9+9, (x2-x1)*9+10, 1);
				g2.fillRect(x2*9+9, y1*9, 1, (y2-y1)*9+10);
			}
			if (clone.length>6)
			{
				int x3=clone[6],y3=clone[7],z3=clone[8],
					x4=x3+x2-x1,y4=y3+y2-y1,z4=z3+z2-z1;
				if (lyr>=z3 && lyr<=z4)
				{
					g2.setColor(Colors.copyTo);
					g2.fillRect(x3*9, y3*9, (x4-x3)*9+10, 1);
					g2.fillRect(x3*9, y3*9, 1, (y4-y3)*9+10);
					g2.fillRect(x3*9, y4*9+9, (x4-x3)*9+10, 1);
					g2.fillRect(x4*9+9, y3*9, 1, (y4-y3)*9+10);
				}
			}
		}
		for (int i=r.x/scale/9;(i*9+1)*scale<r.x+r.width && i<x;i++)
			for (int j=r.y/scale/9;(j*9+1)*scale<r.y+r.height && j<y;j++)
				field.draw(i,j,lyr,g2,new Rectangle(i*9+1,j*9+1,8,8));
	}
	private void viewMouseMoved(MouseEvent e, boolean dragged)
	{
		Point pt=e.getPoint();
		lastPX=pt.x; lastPY=pt.y;
		isCtrlDown=e.isControlDown();
		updateTooltip();
		if (lastX<0 || lastY<0) return;
		if (dragged)
		{
			if (cloneMode!=0) return;
			int bi;
			if ((e.getModifiersEx()&MouseEvent.BUTTON1_DOWN_MASK)!=0 &&
				!field.g(lastX,lastY,lyr).ctrl() && !field.g(lastX,lastY,lyr+1).ctrl()) bi=0;
			else if ((e.getModifiersEx()&MouseEvent.BUTTON3_DOWN_MASK)!=0) bi=pal;
			else return;
			place(lastX,lastY,lyr,palArr[bi]);
		}
		else
		{
			if (!isCtrlDown && playing)
			{
				int p=lyr+1;
				if (field.g(lastX,lastY,p)!=Blocks.press) p--;
				if (p!=lyr || field.g(lastX,lastY,p)==Blocks.press)
				{
					field.sp(lastX,lastY,p,10);
					field.update();
					view.repaint();
				}
			}
			setClone(lastX,lastY,lyr);
		}
	}
	private void viewMousePressed(MouseEvent e)
	{
		Point pt=e.getPoint();
		lastPX=pt.x; lastPY=pt.y;
		isCtrlDown=e.isControlDown();
		updateTooltip();
		if (lastX<0 || lastY<0) return;
		if (cloneMode!=0)
		{
			cloneMode++;
			if (cloneMode==3)
			{
				int l=clone[2];
				if (clone[5]<clone[2]) l=clone[5];
				int[] x=new int[]{0,0,0,0,0,0,lastX,lastY,l};
				System.arraycopy(clone,0,x,0,6);
				clone=x;
				setLyr(l);
			}
			else if (cloneMode==4)
			{
				cloneMode=0;
				for (int i=0;i<3;i++)
					if (clone[i+3]>=clone[i]) clone[i+3]+=1-clone[i];
					else {
						int t=clone[i+3];
						clone[i+3]=clone[i]-t+1;
						clone[i]=t;
					}
				field.clone(clone);
				recountRed();
				clone=null;
			}
			view.repaint();
			return;
		}
		int bi=pal;
		switch (e.getButton())
		{
			case 1:
				bi=0;
				int p=lyr+1;
				if (field.g(lastX, lastY, lyr).ctrl()) p--;
				if (p==lyr || field.g(lastX, lastY, lyr+1).ctrl())
				{
					if (field.g(lastX,lastY,p)==Blocks.lever)
						field.sp(lastX,lastY,p,field.p(lastX,lastY,p)?0:16);
					else if (field.g(lastX,lastY,p)==Blocks.button && !field.p(lastX,lastY,p))
						field.sp(lastX,lastY,p,10);
					else if (field.g(lastX,lastY,p)==Blocks.press && !playing)
						field.sp(lastX,lastY,p,field.p(lastX,lastY,p)?0:10);
					field.update();
					view.repaint();
					break;
				}
			case 3:
				place(lastX,lastY,lyr,palArr[bi]);
				break;
			case 2:
				p=lyr+1;
				if (field.g(lastX, lastY, lyr).wall!=0) p--;
				if (p==lyr || field.g(lastX, lastY, lyr+1).wall!=0)
				{
					if (field.g(lastX,lastY,p)==Blocks.doorB) p--;
					int w=field.w(lastX, lastY, p), ow=w;
					do w=++w%5;
					while (!field.s(lastX, lastY, p, w));
					if (ow!=w)
					{
						field.update();
						view.repaint();
					}
				}
		}
	}
	private void paintPView(Graphics g2)
	{
		g2.setColor(Colors.grid);
		Rectangle r = g2.getClipBounds();
		g2.fillRect(r.x, r.y, r.width, r.height);
		Graphics2D g=((Graphics2D)g2.create());
		g.scale(pScale, pScale);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(Colors.hilite);
		g.fillRect(pal*9, 0, 10, 10);
		for (int i=0;i<palArr.length;i++)
		{
			r=new Rectangle(i*9+1,1,8,8);
			Palette p=palArr[i];
			field.draw(0, 0, 0, g, r, p.a,
				p.b==null?Blocks.air:p.b,
				p.c==null?Blocks.air:p.c);
		}
	}
	private void pViewMousePressed(MouseEvent e)
	{
		Point p=e.getPoint();
		int pX=p.x/pScale;
		if (pX%9==0) return;
		pX/=9;
		if (pX>=palArr.length) return;
		pal=pX;
		pView.repaint();
	}
	public void mouseWheelMoved(MouseWheelEvent e)
	{
		if (e.isControlDown())
			addScale(-e.getWheelRotation());
		else
		{
			int l=palArr.length;
			pal=((pal+e.getWheelRotation())%l+l)%l; //have to mod twice because of possible negative nums
			pView.repaint();
		}
	}
	private void frameResized()
	{
		pScale=frame.getContentPane().getWidth()/(palArr.length*9+1);
		if (pScale>5) pScale=5;
		if (pScale<2) pScale=2;
		Dimension d=new Dimension((palArr.length*9+1)*pScale,10*pScale);
		pView.setPreferredSize(d);
		pView.setMaximumSize(d);
		pView.revalidate();
	}

	private void playToggle()
	{
		if (playing)
		{
			pTimer.stop();
			pTimer = null;
			play.setIcon(playII);
			tick.setEnabled(true);
			tickMI.setEnabled(true);
		}
		else
		{
			(pTimer = new Timer(MS_PER_TICK,
				new ButtonAction(ButtonAction.TICKALL))).start();
			play.setIcon(pauseII);
			tick.setEnabled(false);
			tickMI.setEnabled(false);
		}
		playing=!playing;
	}
	public void setClone(int pX, int pY, int pZ)
	{
		if (cloneMode==1) {
			clone[0]=clone[3]=pX;
			clone[1]=clone[4]=pY;
			clone[2]=clone[5]=pZ;
		}
		else if (cloneMode==2) {
			clone[3]=pX; clone[4]=pY; clone[5]=pZ;
		}
		else if (cloneMode==3) {
			clone[6]=pX; clone[7]=pY; clone[8]=pZ;
		}
		if (cloneMode!=0) view.repaint();

	}
	private void setPalette(Palette[] p)
	{
		Palette sel=palArr[pal];
		int wireInd=0;
		pal=-1;
		palArr=p;
		for (int i=0;i<p.length;i++)
			if (p[i]==sel) pal=i;
			else if (p[i]==Palette.wire) wireInd=i;
		if (pal==-1) pal=wireInd;
		Dimension d=new Dimension((palArr.length*9+1)*pScale,10*pScale);
		pView.setPreferredSize(d);
		pView.setMaximumSize(d);
		pView.revalidate();
	}
	public void updateTooltip()
	{
		int pX=lastPX/scale, pY=lastPY/scale;
		if (lastPX<0 || lastPY<0 || 
			pX<0 || pY<0 || pX>x*9 || pY>y*9)
		{
			lastX = lastY = -1;
			tooltip.setVisible(false);
			lLoc.setVisible(false);
			sLoc.setVisible(false);
			return;
		}
		if (pX%9==0 || pY%9==0) return;
		pX/=9; pY/=9;
		lastX=pX; lastY=pY;
		lLoc.setText("X="+lastX+", Y="+lyr+", Z="+lastY);
		lLoc.setVisible(true);
		sLoc.setVisible(true);
		if (!isCtrlDown)
		{
			tooltip.setVisible(false);
			return;
		}
		tooltip.setLocation((lastX*9+10)*scale,(lastY*9+1)*scale);
		tooltip.setVisible(true);
		String s="<html>"+lastX+","+lyr+","+lastY+"<p>";
		if (Field.layers>2) s+=field.g(lastX,lastY,lyr+2).name+" on ";
		if (Field.layers>1) s+=field.g(lastX,lastY,lyr+1).name+" on ";
		s+=field.g(lastX,lastY,lyr).name;
		String[] dir=new String[]{"ground","west","east","south","north"};
		for (int i=0;i<Field.layers;i++)
		{
			int p=lyr+i;
			switch (field.g(lastX,lastY,p))
			{
				case button:
					s+="<p>button: attached to "+dir[field.w(lastX,lastY,p)]+" face"+
						"<p>button: "+(field.p(lastX,lastY,p)?field.gp(lastX,lastY,p)+
							" ticks of power left":"unpowered"); break;
				case doorB: if (i==0) p--; else break;
				case doorA:
					s+="<p>door: hinge at "+
						new String[]{"no","NW","SE","NE","SW"}[field.w(lastX,lastY,p)]+" corner"+
						"<p>door: "+(field.p(lastX,lastY,p)?"":"un")+"powered"; break;
				case torch:
					s+="<p>torch: attached to "+dir[field.w(lastX,lastY,p)]+" face"+
						"<p>torch: "+(field.p(lastX,lastY,p)?"":"un")+"powered"; break;
				case lever:
					s+="<p>switch: attached to "+dir[field.w(lastX,lastY,p)]+" face"+
						"<p>switch: "+(field.p(lastX,lastY,p)?"":"un")+"powered"; break;
				case press:
					s+="<p>pressure plate: "+(field.p(lastX,lastY,p)?(field.gp(lastX,lastY,p)==10?
						"powered":field.gp(lastX,lastY,p)+" ticks of power left"):"unpowered"); break;
				case wire:
					s+="<p>wire: "+(field.p(lastX,lastY,p)?"will carry power for "+
						field.gp(lastX,lastY,p)+" blocks":"unpowered"); break;
				case water:
					s+="<p>water: "+((field.gp(lastX,lastY,p)&15)==0?"source":
						"level "+(8-(field.gp(lastX,lastY,p)&7)))+
						((field.gp(lastX,lastY,p)&8)==0?"":", falling")+
						((field.gp(lastX,lastY,p)&16)==0?" (static)":" (dynamic)");
			}
		}
		tooltip.setText(s);
		tooltip.setSize(tooltip.getPreferredSize());
	}
	public void play(final boolean open) {
		try {
			URL u=getSound(open?"door_open.wav":"door_close.wav");
			if (u==null) return;
			Clip clip = AudioSystem.getClip();
			clip.open(AudioSystem.getAudioInputStream(u));
			clip.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private boolean areYouSure()
	{
		if (modified)
		{
			int answer=JOptionPane.showConfirmDialog(null,"Do you want to save first?",
				"Unsaved changes", JOptionPane.YES_NO_CANCEL_OPTION);
			if (answer==JOptionPane.CANCEL_OPTION) return false;
			else if (answer==JOptionPane.YES_OPTION)
				if (save==null) saveOpenDialog(false,false);
				else save();
		}
		return true;
	}
	private void save()
	{
		try {
			if (playing) playToggle();
			String s=save.getAbsolutePath();
			if (s.endsWith(".rdat"))
				field.save(new File(s));
			else
			{
				if (!s.endsWith(".schematic")) s+=".schematic";
				LevelLoader.save(Viewport.this, s);
			}
			modified = false;
			updateTitle();
		}
		catch (Exception ex) {
			JOptionPane.showMessageDialog(frame, ex.getMessage(),
				"Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	private void export(File f)
	{
		try {
			ImageWriter wr = ImageIO.getImageWritersByFormatName("gif").next();
			ImageOutputStream ios = ImageIO.createImageOutputStream(f);
			wr.setOutput(ios);
			wr.prepareWriteSequence(null);
			for (int i=0;i<z;i++)
			{
				BufferedImage bi = new BufferedImage((x*9+1)*scale, (y*9+1)*scale,
					BufferedImage.TYPE_BYTE_INDEXED, Colors.icm);
				BufferedImage back = new BufferedImage(bi.getWidth(), bi.getHeight(),
					BufferedImage.TYPE_3BYTE_BGR);
				Graphics2D g = back.createGraphics();
				g.scale(scale, scale);
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				for (int j=0;j<y;j++)
					for (int k=0;k<x;k++)
						if (field.g(k,j,i)!=Blocks.shadow ||
							(Field.layers>1 && !field.g(k,j,i+1).air()))
						{
							g.setColor(Colors.grid);
							g.fillRect(k*9,j*9,10,10);
							field.draw(k,j,i,g,new Rectangle(k*9+1,j*9+1,8,8));
						}
				byte[] pix=((DataBufferByte)back.getRaster().getDataBuffer()).getData(), ind=new byte[pix.length/3];
				byte[] baR=new byte[Colors.icm.getMapSize()]; Colors.icm.getReds(baR);
				byte[] baG=new byte[Colors.icm.getMapSize()]; Colors.icm.getGreens(baG);
				byte[] baB=new byte[Colors.icm.getMapSize()]; Colors.icm.getBlues(baB);
				for (int j=0;j<ind.length;j++)
				{
					byte n=0;
					int d=Integer.MAX_VALUE;
					for (int k=0;k<baR.length;k++)
					{
						int dr=(baR[k]&0xFF)-(pix[3*j+2]&0xFF),
						    dg=(baG[k]&0xFF)-(pix[3*j+1]&0xFF),
						    db=(baB[k]&0xFF)-(pix[3*j  ]&0xFF),
						    dc=dr*dr+dg*dg+db*db;
						if (dc<d) {n=(byte)k; d=dc;}
					}
					ind[j]=n;
				}
				bi.setData(Raster.createRaster(new PixelInterleavedSampleModel(
					DataBuffer.TYPE_BYTE, bi.getWidth(), bi.getHeight(), 1, bi.getWidth(),
					new int[]{0}), new DataBufferByte(ind, ind.length), null));

				// --------------- write GIF stuff -----------------
				IIOMetadata m = wr.getDefaultImageMetadata(
					new ImageTypeSpecifier(bi), wr.getDefaultWriteParam());

				Node root = m.getAsTree(m.getNativeMetadataFormatName());

				// find the GraphicControlExtension node
				for (Node n=root.getFirstChild(); n!=null; n=n.getNextSibling())
					if (n.getNodeName().equals("GraphicControlExtension")) {
						IIOMetadataNode gce = (IIOMetadataNode)n;
						gce.setAttribute("userInputFlag", "FALSE");
						gce.setAttribute("disposalMethod", "restoreToBackgroundColor");
						gce.setAttribute("delayTime", GIF_DELAY + "");
						break;
					}

				// only the first node needs the ApplicationExtensions node
				if (i == 0) {
					IIOMetadataNode aes = new IIOMetadataNode("ApplicationExtensions");
					IIOMetadataNode ae = new IIOMetadataNode("ApplicationExtension");
					ae.setAttribute("applicationID", "NETSCAPE");
					ae.setAttribute("authenticationCode", "2.0");
					ae.setUserObject(new byte[]{1,0,0});
					aes.appendChild(ae);
					root.appendChild(aes);
				}

				try {m.setFromTree(m.getNativeMetadataFormatName(), root);}
				catch (IIOInvalidTreeException e) {throw e;}
				wr.writeToSequence(new IIOImage(bi, null, m), (ImageWriteParam)null);
				// ------------- end write GIF stuff ---------------
			}
			wr.endWriteSequence();
			ios.close();
		}
		catch (Exception e) {e.printStackTrace();}
	}
	private void saveOpenDialog(boolean open, final boolean gif)
	{
		JFileChooser fc=new JFileChooser();
		fc.setCurrentDirectory(folder);
		fc.setFileFilter(new FileFilter() {
			public String getDescription() {
				if (gif) return "GIF files (.gif)";
				else return "Supported File Types (.rdat, .schematic)";
			}
			public boolean accept(File f) {
				if (f.isDirectory()) return true;
				String s=f.getName().substring(f.getName().lastIndexOf('.')+1);
				if (gif) return s.equals("gif");
				else return s.equals("rdat") || s.equals("schematic");
			}
		});
		if (open) {
			if (fc.showOpenDialog(frame)==JFileChooser.APPROVE_OPTION)
				try {
					save=fc.getSelectedFile();
					folder=save.getParentFile();
					if (save.getName().endsWith("rdat"))
						field.load(save);
					else
						LevelLoader.load(Viewport.this,save.getAbsolutePath());
					modified = false;
					updateTitle();
				}
				catch (Exception ex) {
					JOptionPane.showMessageDialog(frame, ex.getMessage(),
						"Error", JOptionPane.ERROR_MESSAGE);
				}
		}
		else if (fc.showSaveDialog(frame)==JFileChooser.APPROVE_OPTION) {
			if (gif)
			{
				String s = fc.getSelectedFile().getAbsolutePath();
				if (!s.toLowerCase().endsWith(".gif")) s+=".gif";
				export(new File(s));
			}
			else {
				save=fc.getSelectedFile();
				folder=save.getParentFile();
				save();
			}
		}
	}
	public void modify()
	{
		if (!modified)
		{
			modified = true;
			updateTitle();
		}
	}
	private void updateTitle()
	{
		String m="";
		if (modified && !IS_MAC) m="*";
		frame.setTitle(m+(save!=null?save.getName()+" - ":"")+title);
		if (IS_MAC)
			frame.getRootPane().putClientProperty("windowModified", new Boolean(modified));
	}
	public static void main(String[] args) throws Exception
	{
		new Viewport(iX, iY, iZ);
	}
	public class ButtonAction extends AbstractAction
	{
		public static final int NEW=0, CLONE=1,
			SAVE=2, OPEN=3, TICK=4, TICKALL=5, PLAYPAUSE=6,
			LYR_UP=7, LYR_DN=8, ADJUST=9, OPT=10, CLONE_ESC=11,
			ZOOM_IN=12, ZOOM_OUT=13, EXIT=14, SAVEAS=15, GIF=16;
		
		public int vers;
		public ButtonAction(int v) {vers=v;}
		public void actionPerformed(ActionEvent e) { doAction(); }
		private void doAction() {
			switch (vers)
			{
				case NEW:
					if (!areYouSure()) return;
					modified = false;
					save = null;
					field = new Field(Viewport.this,iX,iY,iZ);
					setSize(iX,iY,iZ);
					setLyr(0);
					recountRed();
					for (int i=0;i<palArr.length;i++)
						if (palArr[i]==Palette.wire)
							pal = i;
					view.repaint();
					pView.repaint();
					updateTooltip();
					updateTitle();
					break;
				case OPEN:
					if (!areYouSure()) return;
					saveOpenDialog(true,false);
					break;
				case SAVE:
					if (save!=null) {save(); break;}
				case SAVEAS:
					saveOpenDialog(false,false);
					break;
				case GIF:
					saveOpenDialog(false,true);
					break;
				case CLONE:
					cloneMode=1;
					clone=new int[6];
					break;
				case TICK:
					if (playing) break;
				case TICKALL:
					field.tick();
					view.repaint();
					break;
				case PLAYPAUSE:
					playToggle();
					break;
				case LYR_UP:
					lyr+=2;
				case LYR_DN:
					setLyr(lyr-1);
					view.repaint();
					updateTooltip();
					break;
				case ADJUST:
					adjF.setVisible(true);
					break;
				case OPT:
					optF.setVisible(true);
					break;
				case CLONE_ESC:
					cloneMode=0;
					clone=null;
					view.repaint();
					break;
				case ZOOM_IN:
					addScale(1);
					break;
				case ZOOM_OUT:
					addScale(-1);
					break;
				case EXIT:
					if (areYouSure())
						System.exit(0);
			}
		}
	}
	public class OptionAction implements ItemListener {
		public static final int CYCLIC=0, NEW_WIRE=1,
			DUMMY_SW=2, LAYER1=3, LAYER3=4, BRIDGE=5,
			WATER=6;
	
		public int vers;
		public OptionAction(int v) {vers=v;}
		public void itemStateChanged(ItemEvent e) {
			switch (vers)
			{
				case CYCLIC:
					Field.cyclic=e.getStateChange()==ItemEvent.SELECTED;
					field.update();
					break;
				case NEW_WIRE:
					Field.MCwires=e.getStateChange()==ItemEvent.DESELECTED;
					break;
				case DUMMY_SW:
					Field.dummyGdValve=e.getStateChange()==ItemEvent.DESELECTED;
					field.update();
					break;
				case LAYER1:
					if (e.getStateChange()==ItemEvent.SELECTED)
					{
						c3Lyr.setSelected(false);
						cBridge.setEnabled(false);
						cWater.setSelected(false);
						Field.layers=1;
						setPalette(Palette.pal1);
					}
					else
					{
						Field.layers=2;
						setPalette(Palette.pal2);
					}
					break;
				case LAYER3:
					if (e.getStateChange()==ItemEvent.SELECTED)
					{
						c1Lyr.setSelected(false);
						cBridge.setEnabled(true);
						cWater.setSelected(false);
						Field.layers=3;
						setPalette(Palette.pal3);
					}
					else
					{
						cBridge.setEnabled(false);
						Field.layers=2;
						setPalette(Palette.pal2);
					}
					break;
				case BRIDGE:
					Field.bridge=e.getStateChange()==ItemEvent.SELECTED;
					break;
				case WATER:
					if (waterMode=e.getStateChange()==ItemEvent.SELECTED)
					{
						c1Lyr.setSelected(false);
						c3Lyr.setSelected(false);
						cBridge.setEnabled(false);
						Field.layers=1;
						setPalette(Palette.waterP);
					}
					else
						c1Lyr.setSelected(true);
					break;
			}
			view.repaint();
		}
	}
	public static class StatusBar extends JPanel
	{
		public StatusBar()
		{
			super();
			setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
			setPreferredSize(new Dimension(getPreferredSize().width,23));
			Color c = getBackground();
			if (c==null) c=new Color(0xDDDDDD);
			setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(1, 0, 0, 0, c.darker().darker()),
				BorderFactory.createLineBorder(c.brighter())));
			super.add(Box.createHorizontalStrut(5));
		}
		public JSeparator addSeparator()
		{
			JPanel p=new JPanel(new BorderLayout());
			JSeparator s=new JSeparator(SwingConstants.VERTICAL);
			p.setMaximumSize(new Dimension(2, Integer.MAX_VALUE));
			p.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));
			p.add(s);
			add(p);
			return s;
		}
		public Component add(Component comp) {
			Component c = super.add(comp);
			super.add(Box.createHorizontalStrut(5));
			return c;
		}
		public void addGlue()
		{
			remove(getComponentCount()-1);
			super.add(Box.createHorizontalGlue());
		}
	}
	public class LAFActionListener implements ActionListener {
		String laf;
		public LAFActionListener(String s) {laf=s;}
		public void actionPerformed(ActionEvent e) {
			try {
				UIManager.setLookAndFeel(laf);
				SwingUtilities.updateComponentTreeUI(frame);
				SwingUtilities.updateComponentTreeUI(optF);
				SwingUtilities.updateComponentTreeUI(adjF);
				optF.pack();
				adjF.pack();
			} catch (Exception ex) {
				((Component)e.getSource()).setEnabled(false);
			}
		}
	}
	/**
	 * The "missing icon" is a white box with a black border and a red x.
	 * It's used to display something when there are issues loading an
	 * icon from an external location.
	 *
	 * @author Collin Fagan
	 */
	public class MissingIcon implements Icon {

		private int width, height;
		private BasicStroke stroke = new BasicStroke(4);

		public MissingIcon(int w, int h) {width=w; height=h;}
		public void paintIcon(Component c, Graphics g, int x, int y) {
			Graphics2D g2d = (Graphics2D)g.create();

			g2d.setColor(Color.WHITE);
			g2d.fillRect(x + 1, y + 1, width - 2, height - 2);

			g2d.setColor(Color.BLACK);
			g2d.drawRect(x + 1, y + 1, width - 2, height - 2);

			g2d.setColor(Color.RED);
			g2d.setStroke(stroke);
			double fac=.35;
			int wf=(int)(width*fac), hf=(int)(height*fac);
			g2d.drawLine(x + wf, y + hf,
				x + width - wf, y + height - hf);
			g2d.drawLine(x + wf, y + height - hf, x + width - wf, y + hf);

			g2d.dispose();
		}
		public int getIconWidth() {return width;}
		public int getIconHeight() {return height;}
	}
}
