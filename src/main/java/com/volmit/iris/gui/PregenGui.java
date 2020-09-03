package com.volmit.iris.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.volmit.iris.Iris;
import com.volmit.iris.util.ChunkPosition;
import com.volmit.iris.util.J;
import com.volmit.iris.util.KMap;
import com.volmit.iris.util.M;
import com.volmit.iris.util.PregenJob;

public class PregenGui extends JPanel
{
	private PregenJob job;
	private static final long serialVersionUID = 2094606939770332040L;
	private KMap<ChunkPosition, Color> queue = new KMap<>();
	private int res = 8192;
	private BufferedImage image = new BufferedImage(res, res, BufferedImage.TYPE_INT_RGB);

	public PregenGui()
	{

	}

	@Override
	public void paint(Graphics gx)
	{
		double minC = Math.floorDiv(job.min(), 16) - 4;
		double maxC = Math.floorDiv(job.max(), 16) + 4;

		Graphics2D g = (Graphics2D) gx;
		Graphics2D bg = (Graphics2D) image.getGraphics();

		for(ChunkPosition i : queue.k())
		{
			draw(i, queue.get(i), minC, maxC, bg);
		}

		queue.clear();

		g.drawImage(image, 0, 0, getParent().getWidth(), getParent().getHeight(), new ImageObserver()
		{
			@Override
			public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height)
			{
				return true;
			}
		});

		J.a(() ->
		{
			J.sleep((long) 500);
			repaint();
		});
	}

	private void draw(ChunkPosition p, Color c, double minC, double maxC, Graphics2D bg)
	{
		double pw = M.lerpInverse(minC, maxC, p.getX());
		double ph = M.lerpInverse(minC, maxC, p.getZ());
		double pwa = M.lerpInverse(minC, maxC, p.getX() + 1);
		double pha = M.lerpInverse(minC, maxC, p.getZ() + 1);
		int x = (int) M.lerp(0, res, pw);
		int z = (int) M.lerp(0, res, ph);
		int xa = (int) M.lerp(0, res, pwa);
		int za = (int) M.lerp(0, res, pha);
		bg.setColor(c);
		bg.fillRect(x, z, xa - x, za - z);
	}

	private static void createAndShowGUI(PregenJob j)
	{
		JFrame frame = new JFrame("Pregen View");
		PregenGui nv = new PregenGui();
		nv.job = j;
		j.subscribe((c, b) -> nv.queue.put(c, b));
		frame.add(nv);
		frame.setSize(1440, 820);
		frame.setVisible(true);
		File file = Iris.getCached("Iris Icon", "https://raw.githubusercontent.com/VolmitSoftware/Iris/master/icon.png");

		if(file != null)
		{
			try
			{
				frame.setIconImage(ImageIO.read(file));
			}

			catch(IOException e)
			{

			}
		}
	}

	public static void launch(PregenJob g)
	{
		J.a(() ->
		{
			createAndShowGUI(g);
		});
	}
}
