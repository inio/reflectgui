package util;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.*;

public final class Utils {
	private Utils() {}
	
	public static Class<?> typeClass(Type t) {
		if (t == null)
			return null;
		if(t instanceof Class)
			return (Class<?>) t;
		if(t instanceof ParameterizedType)
			return typeClass(((ParameterizedType) t).getRawType());
		throw new REThrow("unknown type "+t);
	}

	public static Type box(Type t) {
		if (t == Integer.TYPE) return Integer.class;
		if (t == Byte.TYPE) return Byte.class;
		if (t == Short.TYPE) return Short.class;
		if (t == Long.TYPE) return Long.class;
		if (t == Boolean.TYPE) return Boolean.class;
		if (t == Character.TYPE) return Character.class;
		if (t == Float.TYPE) return Float.class;
		if (t == Double.TYPE) return Double.class;
		return t;
	}
	
	public static Type unbox(Type t) {
		if (t == Integer.class) return Integer.TYPE;
		if (t == Byte.class) return Byte.TYPE;
		if (t == Short.class) return Short.TYPE;
		if (t == Long.class) return Long.TYPE;
		if (t == Boolean.class) return Boolean.TYPE;
		if (t == Character.class) return Character.TYPE;
		if (t == Float.class) return Float.TYPE;
		if (t == Double.class) return Double.TYPE;
		return t;
	}
	
	public static void copy(File src, File dst) throws IOException {
		FileInputStream in = new FileInputStream(src);
		FileOutputStream out = new FileOutputStream(dst);
    
        // Transfer bytes from in to out
        byte[] buf = new byte[1024*128];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }
	
	public static void aliasEdge(BufferedImage img, int cutoff) {
		for(int y=0 ; y<img.getHeight() ; y++) {
			for(int x=0 ; x<img.getWidth() ; x++) {
				int c = img.getRGB(x,y);
				if (c == 0) continue;
				int a = (c>>24)&0x000000FF;
				if (a == 255) continue;
				if (a < cutoff) {
					img.setRGB(x, y, 0);
					continue;
				}
				int r = (c>>16)&0x000000FF;
				int g = (c>>8)&0x000000FF;
				int b = (c>>0)&0x000000FF;
				if (img.isAlphaPremultiplied()) {
					r = r*255/a;
					if (r > 255) r = 255;
					g = g*255/a;
					if (g > 255) g = 255;
					b = b*255/a;
					if (b > 255) b = 255;
				}
				img.setRGB(x, y, (255<<24)|(r<<16)|(g<<8)|(b<<0));
			}
		}
	}

	public static Rectangle getBounds(BufferedImage color) {
		return getBounds(color, 0, 0, 0, 0);
	}
	public static Rectangle getBounds(BufferedImage color, int cropt, int cropb, int cropl, int cropr) {
		int width = color.getWidth();
		int height = color.getHeight();
		int l=width, r=0, t=height, b=0;
		
		for(int row=cropt ; row<height && t == height ; row++) {
			for(int col=0 ; col<width ; col++) {
				if ((color.getRGB(col, row) & 0xFF000000) != 0) {
					t = row;
					break;
				}
			}
		}
		for(int row=height-1-cropb ; row>t && b == 0 ; row--) {
			for(int col=0 ; col<width ; col++) {
				if ((color.getRGB(col, row) & 0xFF000000) != 0) {
					b = row+1;
					break;
				}
			}
		}
		if (t>b) {
			return null;
		}
		for(int col=cropl ; col<width && l == width ; col++) {
			for(int row=t ; row<b ; row++) {
				if ((color.getRGB(col, row) & 0xFF000000) != 0) {
					l = col;
					break;
				}
			}
		}
		for(int col=width-1-cropr ; col>l && r == 0 ; col--) {
			for(int row=t ; row<b ; row++) {
				if ((color.getRGB(col, row) & 0xFF000000) != 0) {
					r = col+1;
					break;
				}
			}
		}
		
		return new Rectangle(l, t, r-l, b-t);
	}

	public static int colorTo1555(Color color) {
		return rgbTo1555(color.getRGB());
	}

	public static int colorTo565(Color color) {
		return rgbTo565(color.getRGB());
	}

	public static int rgbTo1555(int rgb) {
		int out = 0;
		out |= ((rgb>>(8-5))&0x1F)<<0;
		out |= ((rgb>>(16-5))&0x1F)<<5;
		out |= ((rgb>>(24-5))&0x1F)<<10;
		return out;
	}

	public static int rgbTo565(int rgb) {
		int out = 0;
		out |= ((rgb>>(8-5))&0x1F)<<0;
		out |= ((rgb>>(16-6))&0x3F)<<5;
		out |= ((rgb>>(24-5))&0x1F)<<11;
		return out;
	}
}
