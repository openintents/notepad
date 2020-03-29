/*

Originally from MIT-licensed
https://github.com/colormine/colormine/blob/master/colormine/src/main/org/colormine/colorspace/ColorSpaceConverter.java
  by Joe Zack <me@joezack.com>
  Retrieved 2020-03-28; retrieved copy was last modified 2012-04-09 (74be660).

*/

package org.openintents.notepad.util;

import java.awt.Color;
import java.util.Arrays;

/**
 * Handles all of the math for converting between color spaces
 */

public class ColorConverter {

	/**
	 * Converts a java.awt.Color to Lab color space
	 * 
	 * @param color
	 * @return color in Lab color space
	 */
	public static Lab colorToLab(Color color) {
		Xyz xyz = colorToXyz(color);
		return xyzToLab(xyz);
	}

	/**
	 * Converts a java.awt.Color to Xyz color space
	 * 
	 * @param color
	 * @return color in Xyz color space
	 */
	public static Xyz colorToXyz(Color color) {
		double r = pivotRgb(color.getRed() / 255.0);
		double g = pivotRgb(color.getGreen() / 255.0);
		double b = pivotRgb(color.getBlue() / 255.0);

		// Observer. = 2°, Illuminant = D65
		return new Xyz(r * 0.4124 + g * 0.3576 + b * 0.1805, r * 0.2126 + g * 0.7152 + b * 0.0722, r * 0.0193 + g * 0.1192 + b * 0.9505);
	}

	/**
	 * Converts Xyz to Lab color space
	 * 
	 * @param xyz
	 * @return color in Lab color space
	 */
	public static Lab xyzToLab(Xyz xyz) {
		final double REF_X = 95.047; // Observer= 2°, Illuminant= D65
		final double REF_Y = 100.000;
		final double REF_Z = 108.883;

		double x = pivotXyz(xyz.X / REF_X);
		double y = pivotXyz(xyz.Y / REF_Y);
		double z = pivotXyz(xyz.Z / REF_Z);

		return new Lab(116 * y - 16, 500 * (x - y), 200 * (y - z));
	}

	/**
	 * Converts a java.awt.Color to Hsl color space
	 * 
	 * @param color
	 * @return color in Hsl color space
	 */
	public static Hsl colorToHsl(Color color) {

		double R = (color.getRed() / 255.0);
		double G = (color.getGreen() / 255.0);
		double B = (color.getBlue() / 255.0);

		double var_Min = min(R, G, B); // Min. value of RGB
		double var_Max = max(R, G, B); // Max. value of RGB
		double delta_Max = var_Max - var_Min; // Delta RGB value

		double l = 0.0;
		double h = 0.0;
		double s = 0.0;

		l = (var_Max + var_Min) / 2;

		if (delta_Max == 0.0) {
			h = 0.0;
			s = 0.0;
		} else {
			s = (l < 0.5) ? delta_Max / (var_Max + var_Min) : delta_Max / (2.0 - var_Max - var_Min);

			double del_R = (((var_Max - R) / 6.0) + (delta_Max / 2.0)) / delta_Max;
			double del_G = (((var_Max - G) / 6.0) + (delta_Max / 2.0)) / delta_Max;
			double del_B = (((var_Max - B) / 6.0) + (delta_Max / 2.0)) / delta_Max;

			if (areCloseEnough(R, var_Max)) {
				h = del_B - del_G;
			} else if (areCloseEnough(G, var_Max)) {
				h = (1.0 / 3.0) + del_R - del_B;
			} else if (areCloseEnough(B, var_Max)) {
				h = (2.0 / 3.0) + del_G - del_R;
			}

			if (h < 0.0) {
				h += 1.0;
			}

			if (h > 1.0) {
				h -= 1.0;
			}

		}
		return new Hsl(h, s, l);
	}

	/**
	 * Converts a Hsl color space to java.awt.Color
	 * 
	 * @param hsl
	 * @return color as java.awt.Color
	 */

	public static Color hslToColor(Hsl hsl) {
		double r = 0;
		double g = 0;
		double b = 0;
		double var_1 = 0.0;
		double var_2 = 0.0;

		if (hsl.S == 0.0) {
			r = hsl.L * 255;
			g = hsl.L * 255;
			b = hsl.L * 255;
		} else {
			if (hsl.L < .5) {
				var_2 = hsl.L * (1.0 + hsl.S);
			} else {
				var_2 = (hsl.L + hsl.S) - (hsl.S * hsl.L);
			}

			var_1 = 2.0 * hsl.L - var_2;

			r = (255 * hueToRgb(var_1, var_2, hsl.H + (1.0 / 3.0)));
			g = (255 * hueToRgb(var_1, var_2, hsl.H));
			b = (255 * hueToRgb(var_1, var_2, hsl.H - (1.0 / 3.0)));

		}

		return new Color((int) r, (int) g, (int) b);
	}

	/**
	 * Determines if colors are "close enough" given a tolerance
	 * 
	 * @param firstColor
	 * @param secondColor
	 * @param nearMatchTolerance
	 * @return true if colors are "close enough"
	 */

	public static boolean isNearMatch(Color firstColor, Color secondColor, double nearMatchTolerance) {

		int[] values = { firstColor.getRed(), firstColor.getGreen(), firstColor.getBlue() };
		int[] values2 = { secondColor.getRed(), secondColor.getGreen(), secondColor.getBlue() };

		return compareNearValue(values[0], values2[0], nearMatchTolerance) && compareNearValue(values[1], values2[1], nearMatchTolerance) && compareNearValue(values[2], values2[2], nearMatchTolerance);
	}

	public static boolean isNearMatch(ColorTuple firstColor, ColorTuple secondColor, double nearMatchTolerance) {
		Double[] values = firstColor.getTuple();
		Double[] values2 = secondColor.getTuple();
		return compareNearValue(values[0], values2[0], nearMatchTolerance) && compareNearValue(values[1], values2[1], nearMatchTolerance) && compareNearValue(values[2], values2[2], nearMatchTolerance);
	}

	private static boolean compareNearValue(double value, double otherValue, double nearMatchTorrerance) {
		return value == otherValue || Math.abs(value - otherValue) <= nearMatchTorrerance;
	}

	private final static double DoublePrecision = .000001;

	private static boolean areCloseEnough(double a, double b) {
		return Math.abs(a - b) < DoublePrecision;
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
	};

	private static double pivotRgb(double n) {
		return (n > 0.04045 ? Math.pow((n + 0.055) / 1.055, 2.4) : n / 12.92) * 100;
	}

	private static double pivotXyz(double n) {
		double i = Math.cbrt(n);
		return n > 0.008856 ? i : 7.787 * n + 16 / 116;
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
