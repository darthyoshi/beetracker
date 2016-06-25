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

varying vec4 vertTexCoord;
uniform vec4 exitParams;

/**
 * Determines if the texel is within the circle.
 */
bool isInExit() {
  return pow((exitParams[0]-vertTexCoord.s)/exitParams[2],2.0) +
    pow((1.0-exitParams[1]-vertTexCoord.t)/exitParams[3],2.0) < 1.0;
}

void main() {
  vec4 col = texture2D(texture, vertTexCoord.st);

  gl_FragColor = isInExit() ? col : vec4(0.0,0.0,0.0,0.0);
}
