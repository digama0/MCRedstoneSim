package com.carneiro.mcredsim;

public enum Blocks {
	air(0,false,"air"),
	block(0,false,"block"),
	wire(0,true,"wire"),
	torch(1,true,"torch"),
	lever(1,true,"switch"),
	button(3,true,"button"),
	doorA(2,true,"door"),
	doorB(2,true,"door"),
	press(0,true,"pressure pad"),
	sand(0,false,"sand"),
	water(0,false,"water"),
	shadow(0,false,"shadow");
	public final byte wall; // wall types: 0 = 0 only / attached below or nowhere
	public final boolean conn;          // 1 = 0-4 / attached to wall
	public final String name;           // 2 = 1-4 / attached below
	Blocks(int w, boolean c,String s)   // 3 = 1-4 / attached to wall
	{wall=(byte)w; conn=c; name=s;}
	public boolean ctrl() {return this==lever || this==button || this==press;}
	public boolean block() {return this==block || this==sand;}
	public boolean air() {return this==air || this==shadow;}
	public boolean destruct()
		{return !block() && this!=press && this!=doorA && this!=doorB && this!=water;} // destroyed by water? 
}
