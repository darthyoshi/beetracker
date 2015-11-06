#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

#define PROCESSING_TEXTURE_SHADER

uniform sampler2D texture;

varying vec4 vertTexCoord;

uniform bool keepVisible;

void main() {
  vec4 col = texture2D(texture, vertTexCoord.st);

  if(keepVisible) {
    gl_FragColor = vec4(col.r, col.g, col.b, 0.5);
  }

  else {
    if(col.a < 1.0) {
      gl_FragColor = vec4(0, 0, 0, 0);
    }
  }
}