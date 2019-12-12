Redstone Simulator v2.2

This program simulates the behavior of redstone in Minecraft.

----- Controls -----

Left click to erase tiles or activate switches.
Right click to place the active material on the map.
Middle click on torches or switches to change the wall they are attached to.
Use the scroll wheel to select a material from the palette.
Hold the control key and hover the mouse over any tile in the field for more information on it.

----- Menus -----


-- File --

New: reset field to empty 30x20x7 map.

Save/Open: save and load .rdat files to keep designs between runs of the program. You can also open .schematic files (exported by MCEdit) and .dat chunk files from the game.

Export as GIF: creates a GIF file containing the entire design, animated to show the layers (bottom to top). Each frame stays for 2 seconds.

Exit: Take a wild guess.

-- Edit --

Clone: copy parts of a map. Click once to set one corner, again to set the diagonally opposite corner (use the layer buttons to make a tall selection box), and once more to set the bottom corner of the paste region. Press Escape to stop cloning before finished. WARNING: separate torches from their blocks, chop doors in half, etc. at your own risk.

Tick - step forward in time by one "tick" - that is, the time for a torch to turn on or off from its previous state.
Play - tick continually (at 10 ticks per second)
Pause - stop simulation

Options...: Opens the Options dialog. (See below.)

Look and Feel: Choose another interface style from the menu if you prefer.

-- View --

Level up/down: Press these buttons (or up and down arrow keys, or W and S) to go through horizontal slices. Level 1 is ground level.

-- Adjust Size --

Adjust Size: Click this button to open a separate panel.

These 12 buttons move each face of the cuboid region in or out, creating more or less space to work in. The circled dot is an arrow pointing at you, and the circled plus is an arrow pointing away from you. If you shrink the area where you have placed components, the program will warn you, unless you hold Shift while doing so.

The two menus do the same thing, but are accessible directly through the menu, or through the numpad key combinations.

----- Options -----
There are several options in this dialog for fine-tuning the interface. In order:

Cyclic (in X and Z only) - torches, wires, power, and everything else "bleeds" over the edges and wraps around. You can even connect a torch to a block on the other side of the field!

"Natural" wire connections - A modification of Minecraft's wire connection algorithm, intended to help visualize the connections without extraneous connections. Wires only point in a direction if they are receiving power from or are powering the tile. To avoid confusion, unconnected wires form "squares" instead of crosses and singly-connected wires don't extend all the way across the tile. That way, screenshots in this mode should be distinctive enough that the different pointing direction doesn't confuse anyone who may be expecting something else.

Ground switches power blocks below - In Minecraft, there are two kinds of ground switches -- E/W, and N/S. One of them powers the block below them, and the other doesn't. This option toggles the behavior of ground switches to behave in on mode or the other.

Show only one layer / Show three layers - deselect both checkboxes for 2 layer mode. In 1 layer mode, no 2-layer palette options are available, and only 1 layer is visible. In 2 layer mode, bridges and wire-block-blocks are not visible. In 3 layer mode, all available tiles are visible.

Show bridges - Enable this option to have a wire on a gray background mean "wire over block over wire" or disable it to show "wire over block over air" instead. They cannot both be viewed simultaneously, as that would cause ambiguity.

Block type - This will be used when exporting to .schematic files. Choose which block you would like the "generic block" to actually become. (Grass/Dirt grows grass everywhere it can, and uses dirt everywhere else.)

----- Main view -----

This is where you make your design. Use the controls described earlier to draw.

Note that you actually view 2 layers at once: the level stated in the toolbar, and the one above that. this is so that you can see most 2D designs. See Palette.


----- Palette -----

Click on the buttons to select the active material, or use the mouse wheel. From left to right, the materials are:

Air: air over air
Block: air over block
Wire: air over wire (note that this will also place a block below, if necessary)
Torch: air over torch (middle click to change orientation)
Switch: air over switch
Button: air over button (button lasts 10 ticks)
Wire over Block
Torch over Block
Switch over Block
Bridge: wire over block over wire (this is the only palette option that will change anything 2 levels above, but is provided for convenience in viewing and editing)
Block over Wire
Block over Torch
Block over Switch
Door: 2-high (unlike Minecraft, this door actually makes the right sounds for open and close, rather than random)
Shadow: special block (see below)

Note that not all combinations of two materials stacked are represented here. In these cases (like torch  over torch, switch over plate) only the bottom object will be visible (although you can always use the level up button to get a complete picture).

The shadow block appears the same color as the grid, but is functionally equivalent to air. It is good for drawing the bounds of an irregularly shaped circuit. When exported as GIF, it appears transparent.

----- Status Bar -----

Layer number: This shows what layer you are on. The numbering starts at 1 at the lowest level and goes up from there. If it is blue, you are above or at ground level. If you are underground (by using the "Back (ground) face down" menu item or button), it will be brown.

Size: Number of blocks long, wide, and high. Order is X,Z,Y (for backwards compatibility).

Location: Only visible if the cursor is over the field. Shows the X.Y.Z position of the mouse (with 0-based indices).

Redstone counts (right):
Keep track of your materials budget. The red cross is the number of redstone wires placed, the torch is the number of torches, and the ore is torches + wires = the total amount necessary to mine.

-----

This is still very much a work in progress; please PM or post to Baezon on the Minecraft Forum:

http://www.minecraftforum.net/

to report any behavior which does not match Minecraft redstone, or any bugs, feature requests, or interface ideas that might help me improve this work.
