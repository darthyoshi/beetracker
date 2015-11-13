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
 * Applies a morphological probe to determine the new texel value.
 */
vec4 probeColor() {
  float i, j, x, y;
  vec4 col, blank = vec4(0, 0, 0, 0);

  for(i = -filterRadius; i <= filterRadius; i++) {
    x = i*texOffset.s + vertTexCoord.s;
    for(j = -filterRadius; j <= filterRadius; j++) {
      if(abs(i) + abs(j) <= filterRadius) {
        y = j*texOffset.t + vertTexCoord.t;
        col = texture2D(texture, vec2(x, y));

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