#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

#define PROCESSING_TEXTURE_SHADER

uniform sampler2D texture;

varying vec4 vertTexCoord;

uniform vec3 threshold;
uniform float basehue;

void main() {
  vec4 col = texture2D(texture, vertTexCoord.st);
  //convert RGB to HSV
  float h, s, v = max(max(col.r, col.g), max(col.g, col.b));
  float delta = v - min(min(col.r, col.g), min(col.g, col.b));

  if(delta == 0.0) {
    h = v;
  }

  else if(v == col.r){
    h = mod((col.g-col.b)/delta, 6.0)/6.0;
  }

  else if(v == col.g) {
    h = ((col.b-col.r)/delta+2.0)/6.0;
  }

  else if(v == col.b) {
    h = ((col.r-col.g)/delta+4.0)/6.0;
  }

  if(v == 0.0) {
    s = v;
  }

  else {
    s = delta/v;
  }

  //check if texel color is within thresholds
  if(
    h < threshold[0] + basehue && h > basehue - threshold[0] &&
    s > threshold[1] &&
    v > threshold[2]
  ) {
    //convert HSV to RGB
    float r, g, b, x;
    x = 1 - abs(mod(basehue*6.0, 2.0)-1);

    if(basehue < 1.0/6.0) {
      r = 1;
      g = x;
      b = 0;
    }

    else if(basehue < 1.0/3.0) {
      r = x;
      g = 1;
      b = 0;
    }

    else if(basehue < 0.5) {
      r = 0;
      g = 1;
      b = x;
    }

    else if(basehue < 2.0/3.0) {
      r = 0;
      g = x;
      b = 1;
    }

    else if(basehue < 5.0/6.0) {
      r = x;
      g = 0;
      b = 1;
    }

    else {
      r = 1;
      g = 0;
      b = x;
    }

    gl_FragColor = vec4(r, g, b, 1.0);
  }

  else {
    gl_FragColor = col;
  }
}