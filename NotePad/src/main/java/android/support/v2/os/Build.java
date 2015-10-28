/* 
 * Copyright (C) 2011-2012 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.support.v2.os;

/**
 * Information about the current build, extracted from system properties.
 * 
 * This class ensures backward compatibility down to Android 1.1 (API level 2).
 */
public class Build {
	public static class VERSION {
		public static int SDK_INT = 2;

		static {
			try {
				// Android 1.6 (v4) and higher:
				// access Build.VERSION.SDK_INT.
				SDK_INT = android.os.Build.VERSION.class.getField("SDK_INT")
						.getInt(null);
			} catch (Exception e) {
				try {
					// Android 1.5 (v3) and lower:
					// access Build.VERSION.SDK.
					SDK_INT = Integer
							.parseInt((String) android.os.Build.VERSION.class
									.getField("SDK").get(null));
				} catch (Exception e2) {
					// This should never happen:
					SDK_INT = 2;
				}
			}
		}
	}

	public static class VERSION_CODES {
		/**
		 * Magic version number for a current development build, which has not
		 * yet turned into an official release.
		 */
		public static final int CUR_DEVELOPMENT = 10000;

		/**
		 * October 2008: Android 1.0
		 */
		public static final int BASE = 1;

		/**
		 * February 2009: Android 1.1.
		 */
		public static final int BASE_1_1 = 2;

		/**
		 * May 2009: Android 1.5.
		 */
		public static final int CUPCAKE = 3;

		/**
		 * September 2009: Android 1.6.
		 */
		public static final int DONUT = 4;

		/**
		 * November 2009: Android 2.0
		 * 
		 */
		public static final int ECLAIR = 5;

		/**
		 * December 2009: Android 2.0.1
		 */
		public static final int ECLAIR_0_1 = 6;

		/**
		 * January 2010: Android 2.1
		 */
		public static final int ECLAIR_MR1 = 7;

		/**
		 * June 2010: Android 2.2
		 */
		public static final int FROYO = 8;

		/**
		 * November 2010: Android 2.3
		 */
		public static final int GINGERBREAD = 9;

		/**
		 * February 2011: Android 2.3.3.
		 */
		public static final int GINGERBREAD_MR1 = 10;

		/**
		 * February 2011: Android 3.0.
		 */
		public static final int HONEYCOMB = 11;

		/**
		 * May 2011: Android 3.1.
		 */
		public static final int HONEYCOMB_MR1 = 12;

		/**
		 * June 2011: Android 3.2.
		 */
		public static final int HONEYCOMB_MR2 = 13;

		/**
		 * October 2011: Android 4.0.
		 */
		public static final int ICE_CREAM_SANDWICH = 14;

		/**
		 * Android 4.0.3.
		 */
		public static final int ICE_CREAM_SANDWICH_MR1 = 15;
	}
}
