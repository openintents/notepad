/*

Originally from MIT-licensed
https://github.com/colormine/colormine/blob/master/colormine/src/main/org/colormine/colorspace/ColorSpaceConverter.java
  by Joe Zack <me@joezack.com>
  Retrieved 2020-03-28; retrieved copy was last modified 2012-04-09 (74be660).

Modified 2020-03-29 by Jeremy D Monin <jdmonin@nand.net> for use in OI Notepad:
- For portability use double[] or int[], not Hsl class or AWT Color
- Wrote hslChangeLightness, colorToRgb
- Clarified javadocs
- Cleaned up method names, compiler warnings, unused items, keeping only RGB and HSL colorspaces

The MIT License (MIT)

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
associated documentation files (the "Software"), to deal in the Software without restriction,
including without limitation the rights to use, copy, modify, merge, publish, distribute,
sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

*/

package org.openintents.notepad.util;

import java.util.Arrays;

/**
 * Conversions between color spaces.
 * Also provides {@link #hslChangeLightness(double[], boolean)} and utility methods.
 */
public class ColorConverter {

	/**
	 * In HSL space, the contrast/lightness threshold below which you may want to call
	 * {@link #hslChangeLightness(double[], boolean)}.
	 * @see #areWithinDistance(double, double, double)
	 */
	public static final double LIGHTNESS_THRESHOLD = 1.0 / 3;

	/**
	 * Change this HSL color's lightness by at least {@link #LIGHTNESS_THRESHOLD}
	 * (a non-reversible mapping) to increase contrast.
	 *
	 * @param hsl  color in HSL color space, components in range 0..1
	 * @param avoidDark  True if midrange lightness should be increased, false to decrease
	 * @return  {@code hsl} with its lightness changed by at least {@link #LIGHTNESS_THRESHOLD}
	 */
	public static double[] hslChangeLightness(final double[] hsl, final boolean avoidDark) {
		double L = hsl[2];
		if ((L <= 1.0 / 3) || (L >= (2.0 / 3)))
			L = 1.0 - L;
		else if (avoidDark)
			L += (1.0 / 3);  // (1/3 .. 2/3) -> (2/3 .. 1)
		else
			L -= (1.0 / 3);  // (1/3 .. 2/3) -> (0 .. 1/3)

		hsl[2] = L;
		return hsl;
	}

	/**
	 * Converts an RGB color to HSL color space
	 *
	 * @param rgb color in RGB, components in range 0..255
	 * @return color in HSL color space, components in range 0..1
	 */
	public static double[] rgbToHsl(final int[] rgb) {

		final double R = rgb[0] / 255.0,
			G = rgb[1] / 255.0,
			B = rgb[2] / 255.0;

		double var_Min = min(R, G, B); // Min. value of RGB
		double var_Max = max(R, G, B); // Max. value of RGB
		double delta_Max = var_Max - var_Min; // Delta RGB value

		double l;
		double h = 0.0;
		double s;

		l = (var_Max + var_Min) / 2;

		if (delta_Max == 0.0) {
			h = 0.0;
			s = 0.0;
		} else {
			s = (l < 0.5) ? delta_Max / (var_Max + var_Min) : delta_Max / (2.0 - var_Max - var_Min);

			double del_R = (((var_Max - R) / 6.0) + (delta_Max / 2.0)) / delta_Max;
			double del_G = (((var_Max - G) / 6.0) + (delta_Max / 2.0)) / delta_Max;
			double del_B = (((var_Max - B) / 6.0) + (delta_Max / 2.0)) / delta_Max;

			if (areNearlySame(R, var_Max)) {
				h = del_B - del_G;
			} else if (areNearlySame(G, var_Max)) {
				h = (1.0 / 3.0) + del_R - del_B;
			} else if (areNearlySame(B, var_Max)) {
				h = (2.0 / 3.0) + del_G - del_R;
			}

			if (h < 0.0) {
				h += 1.0;
			}

			if (h > 1.0) {
				h -= 1.0;
			}

		}

		return new double[]{h, s, l};
	}

	/**
	 * Converts a HSL color space to RGB
	 *
	 * @param hsl  color as HSL (components in range 0..1)
	 * @return color as RGB (in range 0..255)
	 */
	public static int[] hslToRgb(double[] hsl) {
		final double H = hsl[0], S = hsl[1], L = hsl[2];
		double r, g, b;

		if (S == 0.0) {
			r = L * 255;
			g = L * 255;
			b = L * 255;
		} else {
			double var_1, var_2;

			if (L < .5) {
				var_2 = L * (1.0 + S);
			} else {
				var_2 = (L + S) - (S * L);
			}

			var_1 = 2.0 * L - var_2;

			r = 255 * hueToRgb(var_1, var_2, H + (1.0 / 3.0));
			g = 255 * hueToRgb(var_1, var_2, H);
			b = 255 * hueToRgb(var_1, var_2, H - (1.0 / 3.0));

		}

		return new int[]{(int) r, (int) g, (int) b};
	}

	/**
	 * Are these values near each other numerically?
	 *
	 * @param value1  Value to compare
	 * @param value2  Value to compare
	 * @param distance  Max allowable distance to be near each other
	 * @return  True if {@code value1} and {@code value2} are near each other.
	 * 	   That is, abs(value1 - value2) &lt;= {@code distance}.
	 */
	public static boolean areWithinDistance(double value1, double value2, double distance) {
		return value1 == value2 || Math.abs(value1 - value2) <= distance;
	}

	private final static double DOUBLE_PRECISION = .000001;

	private static boolean areNearlySame(double a, double b) {
		return Math.abs(a - b) < DOUBLE_PRECISION;
	}

	/**
	 * Decode an Android color int to an {r, g, b} array.
	 * To encode again, call {@link android.graphics.Color#rgb(int, int, int)}.
	 *
	 * @param argbColor  Android color int, as described in {@link android.graphics.Color}
	 * @return Array with r, g, b components in range 0..255
	 */
	public static int[] colorToRgb(int argbColor) {
		final int r = (argbColor >> 16) & 0xff,
		    g = (argbColor >> 8) & 0xff,
		    b = argbColor & 0xff;

		return new int[]{r, g, b};
	}

	private static double hueToRgb(double v1, double v2, double vh) {
		if (vh < 0.0) {
			vh += 1.0;
		}

		if (vh > 1.0) {
			vh -= 1.0;
		}

		if ((6.0 * vh) < 1.0) {
			return (v1 + (v2 - v1) * 6.0 * vh);
		}

		if ((2.0 * vh) < 1.0) {
			return (v2);
		}

		if ((3.0 * vh) < 2.0) {
			return (v1 + (v2 - v1) * ((2.0 / 3.0 - vh) * 6.0));
		}

		return (v1);
	}

	private static double max(double... numbers) {
		Arrays.sort(numbers);
		return numbers[numbers.length - 1];
	}

	private static double min(double... numbers) {
		Arrays.sort(numbers);
		return numbers[0];
	}

}

