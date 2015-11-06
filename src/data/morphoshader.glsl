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
//TODO figure out why loop throws errors
/*  float i, j, x, y;
  vec4 col, blank = vec4(0, 0, 0, 0);

  for(i = -filterRadius; i <= filterRadius; i += 1.0) {
    x = i*texOffset.s;
  //  if(x >= 0.0 && x < 1.0) {
      for(j = -filterRadius; j <= filterRadius; j += 1.0) {
        y = j*texOffset.t;
   //     if(y >= 0.0 && y < 1.0) {
          if(abs(i) + abs(j) <= filterRadius) {
            col = texture2D(texture, vertTexCoord.st + vec2(x, y));

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
 //       }
 //     }
    }
  }

  //dilate: miss, erode: hit
  return dilateMode ? blank : col;*/

  vec4 x = texture2D(texture, vertTexCoord.st);
  vec4 l = texture2D(texture, vertTexCoord.st + vec2(-texOffset.s,          0.0));
  vec4 r = texture2D(texture, vertTexCoord.st + vec2(+texOffset.s,          0.0));
  vec4 u = texture2D(texture, vertTexCoord.st + vec2(         0.0, -texOffset.t));
  vec4 d = texture2D(texture, vertTexCoord.st + vec2(         0.0, +texOffset.t));
  vec4 result, blank = vec4(0, 0, 0, 0);

  if(dilateMode) {
    if(x.a == 1.0) {
       result = x;
    }
    else if(d.a == 1.0) {
       result = d;
    }
    else if(u.a == 1.0) {
       result = u;
    }
    else if(l.a == 1.0) {
       result = l;
    }
    else if(r.a == 1.0) {
       result = r;
    }
    else {
      result = blank;
    }
  }
  else {
    if(x.a == 1.0 && u.a == 1.0 && d.a == 1.0 && l.a == 1.0 && r.a == 1.0) {
      result = x;
    }
    else {
      result = blank;
    }
  }

  return result;
}

void main() {
  gl_FragColor = probeColor();
}