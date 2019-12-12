package com.carneiro.mcredsim;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public enum Palette {
	air(Blocks.air),
	shadow(Blocks.shadow),
	block(Blocks.block),
	wire(Blocks.wire),
	torch(Blocks.torch),
	lever(Blocks.lever),
	button(Blocks.button),
	press(Blocks.press),
	sand(Blocks.sand),
	water(Blocks.water),
	air2(Blocks.air,Blocks.air),
	shadow2(Blocks.shadow,Blocks.shadow),
	blockblock(Blocks.block,Blocks.block),
	blockwire(Blocks.block,Blocks.wire),
	blocktorch(Blocks.block,Blocks.torch),
	blocklever(Blocks.block,Blocks.lever),
	blockpress(Blocks.block,Blocks.press),
	wireblock(Blocks.wire,Blocks.block),
	torchblock(Blocks.torch,Blocks.block),
	leverblock(Blocks.lever,Blocks.block),
	wiretorch(Blocks.wire,Blocks.torch),
	door(Blocks.doorA,Blocks.doorB),
	bridge(Blocks.wire,Blocks.block,Blocks.wire);
	public final Blocks a, b, c;
	Palette(Blocks _a) {this(_a,null,null);}
	Palette(Blocks _a, Blocks _b) {this(_a,_b,null);}
	Palette(Blocks _a, Blocks _b, Blocks _c)
	{a=_a; b=_b; c=_c;}
	public static Palette[] pal1, pal2, pal3;
	public static final Palette[] wireP, waterP =
		new Palette[]{air,block,sand,water,torch,shadow};
	static {
		wireP=new Palette[]{air,air2,
			block,blockblock,wire,torch,blockwire,
			blocktorch,wireblock,torchblock,wiretorch,
			bridge,lever,button,press,door,shadow,shadow2};
		ArrayList<Palette>
			a=new ArrayList<Palette>(),
			a2=new ArrayList<Palette>(),
			a3=new ArrayList<Palette>();
		for (int i=0;i<wireP.length;i++)
		{
			Palette p=wireP[i];
			if (p.b==null) a.add(p);
			if (p.c==null && p!=air && p!=shadow) a2.add(p);
			if (p!=air && p!=shadow) a3.add(p);
		}
		pal1=a.toArray(new Palette[0]);
		pal2=a2.toArray(new Palette[0]);
		pal3=a3.toArray(new Palette[0]);
	}

	@Override
	public String toString() {
		String blocks = Arrays.stream(new Blocks[] { c, b, a }) // Order of blocks is inverted on field for some reason
				.filter(Objects::nonNull)
				.map(x -> x.name)
                .collect(Collectors.joining(" on "));
		return String.format("%s (%s)", blocks, name());
	}
}
