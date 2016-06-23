/*
* BeeTracker
* Copyright (C) 2016 Kay Choi
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

#define PROCESSING_TEXTURE_SHADER

uniform sampler2D texture;
uniform vec2 texOffset;

varying vec4 vertTexCoord;

uniform float filterRadius[2];
uniform bool dilateMode;
uniform vec4 exitParams;

/**
 * Determines if the texel is within the circle.
 */
bool isInExit() {
  return pow((exitParams[0]-vertTexCoord.s)/exitParams[2],2.0) +
    pow((1.0-exitParams[1]-vertTexCoord.t)/exitParams[3],2.0) < 1.0;
}

/**
 * Applies a morphological probe to determine the new texel value.
 */
vec4 probeColor() {
  float i, j, x, y;
  vec4 col, result = (dilateMode ? vec4(0, 0, 0, 0) : texture2D(texture, vertTexCoord.st));
  int index = isInExit() ? 0 : 1;

  for(i = -filterRadius[index]; i <= filterRadius[index]; i++) {
    x = i*texOffset.s + vertTexCoord.s;
    for(j = -filterRadius[index]; j <= filterRadius[index]; j++) {
      if(abs(i) + abs(j) <= filterRadius[index]) {
        y = j*texOffset.t + vertTexCoord.t;
        col = texture2D(texture, vec2(x, y));

        if(dilateMode) {
          //dilate: take texel with greatest intensity
          if(max(max(col.r, col.g), max(col.g, col.b)) >
            max(max(result.r, result.g), max(result.g, result.b))) {
            result = col;
          }
        }
        else if(!dilateMode){
          //erode: take texel with least intensity
          if(max(max(col.r, col.g), max(col.g, col.b)) <
            max(max(result.r, result.g), max(result.g, result.b))) {
            result = col;
          }
        }
      }
    }
  }

  return result;
}

void main() {
  gl_FragColor = probeColor();
}