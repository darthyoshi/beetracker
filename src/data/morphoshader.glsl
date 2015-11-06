#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

#define PROCESSING_TEXTURE_SHADER

uniform sampler2D texture;
uniform vec2 texOffset;

varying vec4 vertTexCoord;

uniform float filterRadius;
uniform bool dilateMode;

/**
 * Applies a morphological probe to determine the new color of a texel.
 */
vec4 probeColor() {
  float i, j, x;
  vec4 col, blank = vec4(0, 0, 0, 0);
  vec2 coords;

  for(i = -filterRadius; i <= filterRadius; i += 1.0) {
    x = i*texOffset.s;
    for(j = -filterRadius; j <= filterRadius; j += 1.0) {
      coords = vertTexCoord.st + vec2(x, j*texOffset.t);
      if(abs(i) + abs(j) <= filterRadius) {
        col = texture2D(texture, coords);

        if(col.a > 0.0) {
          //dilate: hit
          if(dilateMode) {
            return col;
          }
        }
        else if(!dilateMode){
          //erode: miss
          return blank;
        }
      }
    }
  }

  //dilate: miss, erode: hit
  return dilateMode ? blank : col;
}

void main() {
  gl_FragColor = probeColor();
}