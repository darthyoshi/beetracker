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
  vec4 col, result = (dilateMode ? vec4(0, 0, 0, 0) : texture2D(texture, vertTexCoord.st));

  for(i = -filterRadius; i <= filterRadius; i++) {
    x = i*texOffset.s + vertTexCoord.s;
    for(j = -filterRadius; j <= filterRadius; j++) {
      if(abs(i) + abs(j) <= filterRadius) {
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