package com.carneiro.mcredsim;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class LevelLoader {
	public static void load(Viewport p, String world, int cX, int cY)
	{
		load(p,"C:\\Users\\Mario\\AppData\\Roaming\\.minecraft\\saves\\"+world+
			"\\"+Integer.toString(cX&63,36)+"\\"+Integer.toString(cY&63,36)+
			"\\c."+Integer.toString(cX,36)+"."+Integer.toString(cY,36)+".dat");
	}
	public static void load(Viewport p, String file)
	{
		byte[] data=null, extra=null;
		int sX=16,sY=16,sZ=128;
		boolean sch=file.endsWith(".schematic");
		try {
			Tag root=Tag.readFrom(new FileInputStream(file));
			data = (byte[])root.findTagByName("Blocks").getValue();
			extra = (byte[])root.findTagByName("Data").getValue();
			if (sch)
			{
				sX=(Short)root.findTagByName("Width").getValue();
				sY=(Short)root.findTagByName("Length").getValue();
				sZ=(Short)root.findTagByName("Height").getValue();
			}
		}
		catch (Exception e) {}
		byte[][][] chunk = new byte[sZ][][];
		byte[][][] cext = new byte[sZ][][];
		for (int i=0;i<sZ;i++)
		{
			chunk[i]=new byte[sY][];
			cext[i]=new byte[sY][];
			for (int j=0;j<sY;j++)
			{
				chunk[i][j]=new byte[sX];
				cext[i][j]=new byte[sX];
				for (int k=0;k<sX;k++)
				{
					int n;
					byte d;
					if (sch)
					{
						d=(byte)(extra[n=i*sY*sX+j*sX+k]&15);
					}
					else
					{
						d=extra[(n=i+j*sZ+k*sY*sZ)>>1];
						if ((i&1)==0) d=(byte)(d&15);
						else d=(byte)((d&240)>>4);
					}
					switch (data[n])
					{
						case 0: //air
						case 6: //sapling
						case 37: case 38: //flower
						case 39: case 40: //mushroom
						case 51: //fire
						case 59: //crop
						case 63: case 68: //sign
						case 65: //ladder
						case 66: //rails
						case 78: //snow
						case 83: //reeds
							chunk[i][j][k]=(byte)Blocks.air.ordinal(); break;
						case 55:
							cext[i][j][k]=d;
							chunk[i][j][k]=(byte)Blocks.wire.ordinal(); break;
						case 76:
							cext[i][j][k]=16;
						case 75:
							cext[i][j][k]+=new int[]{0,128,96,64,32,0}[d];
							chunk[i][j][k]=(byte)Blocks.torch.ordinal(); break;
						case 69: //lever
							chunk[i][j][k]=(byte)Blocks.lever.ordinal();
							if (d>=8) {d-=8; cext[i][j][k]=16;}
							cext[i][j][k]+=(byte)(new int[]{0,128,96,64,32,0,0}[d]); break;
						case 70: case 72: //plate
							chunk[i][j][k]=(byte)Blocks.press.ordinal();
							cext[i][j][k]=(byte)(new int[]{0,10}[d]); break;
						case 77:
							chunk[i][j][k]=(byte)Blocks.button.ordinal();
							if (d>=8) {d-=8; cext[i][j][k]=16;}
							cext[i][j][k]+=new int[]{32,128,96,64,32}[d]; break;
						case 64: case 71:
							if (d<8) {
								chunk[i][j][k]=(byte)Blocks.doorA.ordinal();
								if (d>=4) {d-=4; cext[i][j][k]=16;}
								cext[i][j][k]+=new int[]{96,64,128,32}[d];
							}
							else {
								chunk[i][j][k]=(byte)Blocks.doorB.ordinal();
								cext[i][j][k]=(byte)0;
							}
							break;
						case 8: //water
						case 10: //lava
							cext[i][j][k]=(byte)16;
						case 9: //still water
						case 11: //still lava
							chunk[i][j][k]=(byte)Blocks.water.ordinal();
							cext[i][j][k]+=d;
						case 12: //sand
						case 13: //gravel
							chunk[i][j][k]=(byte)Blocks.sand.ordinal(); break;
						default:
							chunk[i][j][k]=(byte)Blocks.block.ordinal(); break;
					}
				}
			}
		}
		p.field.data=chunk;
		p.field.extra=cext;
		p.setSize(sX,sY,sZ);
		p.recountRed();
		p.view.repaint();
	}
	public static void save(Viewport p, String file)
	{
		short sZ=(short)p.field.data.length,
		      sY=(short)p.field.data[0].length,
		      sX=(short)p.field.data[0][0].length;
		byte[] blocks=new byte[sX*sY*sZ], data=new byte[sX*sY*sZ];
		for (int i=0,n=0;i<sZ;i++)
			for (int j=0;j<sY;j++)
				for (int k=0;k<sX;k++,n++)
					switch (p.field.g(k,j,i))
					{
						case air:
						case shadow:
							blocks[n]=data[n]=0; break;
						case block:
							if (p.block==2)
								blocks[n]=(byte)(p.field.g(k,j,i+1).block()?3:2);
							else
								blocks[n]=(byte)p.block;
							data[n]=0; break;
						case wire:
							blocks[n]=55; data[n]=p.field.extra[i][j][k]; break;
						case torch:
							blocks[n]=(byte)(p.field.p(k,j,i)?76:75);
							data[n]=(byte)(5-((p.field.extra[i][j][k]>>5)&7));
							break;
						case button:
							blocks[n]=77;
							data[n]=(byte)(new byte[]{0,4,3,2,1}[(
								p.field.extra[i][j][k]>>5)&7]+(p.field.p(k,j,i)?8:0));
							break;
						case lever:
							blocks[n]=69;
							data[n]=(byte)(new byte[]{(byte)(Field.dummyGdValve?6:5),
								4,3,2,1}[(p.field.extra[i][j][k]>>5)&7]+(p.field.p(k,j,i)?8:0));
							break;
						case press:
							blocks[n]=70;
							data[n]=(byte)(p.field.extra[i][j][k]==0?0:1);
							break;
						case doorA:
							blocks[n]=71;
							data[n]=(byte)(new byte[]{0,3,1,0,2}[(
								p.field.extra[i][j][k]>>5)&7]+(p.field.p(k,j,i)?4:0));
							break;
						case doorB:
							blocks[n]=71;
							data[n]=(byte)(data[n-sX*sY]+8);
							break;
						case water:
							blocks[n]=(byte)((p.field.extra[i][j][k]&16)==0?9:8);
							data[n]=(byte)(p.field.extra[i][j][k]&15);
							break;
					}
		try {
			new Tag(Tag.Type.TAG_Compound,"Schematic",new Tag[]{
				new Tag(Tag.Type.TAG_Short,"Height",sZ),
				new Tag(Tag.Type.TAG_Short,"Length",sY),
				new Tag(Tag.Type.TAG_Short,"Width",sX),
				new Tag(Tag.Type.TAG_Byte_Array,"Blocks",blocks),
				new Tag(Tag.Type.TAG_Byte_Array,"Data",data),
				new Tag(Tag.Type.TAG_String,"Materials","Alpha"),
				new Tag("Entities",Tag.Type.TAG_Compound),
				new Tag("TileEntities",Tag.Type.TAG_Compound),
				new Tag(Tag.Type.TAG_End,null,null)}
			).writeTo(new FileOutputStream(file));
		}
		catch (Exception e) {}
	}
	public static void main(String[] args) {
		Viewport v=new Viewport(0, 0, 0);
		//load(v,"World3",28,13);
		load(v, "C:\\Users\\Mario\\Minecraft\\bcd.schematic");
		v.stats.revalidate();
		v.view.revalidate();
		v.frame.pack();
		save(v, "C:\\Users\\Mario\\Minecraft\\bcd2.schematic");
	}
}
