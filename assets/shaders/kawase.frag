#ifdef GL_ES
precision mediump float;
#endif
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform vec2  u_pixelSize;  // vec2(1/texW, 1/texH)
uniform float u_offset;     // pass 0 → 0.5,  pass 1 → 1.5

// Kawase blur: 4 diagonal samples at (±offset, ±offset).
// Two passes at increasing offsets approximate a wide Gaussian at a fraction
// of the texture-fetch cost. Run at quarter-resolution for mobile performance.
void main() {
    vec4 sum = vec4(0.0);
    sum += texture2D(u_texture, v_texCoords + vec2(-u_offset, -u_offset) * u_pixelSize);
    sum += texture2D(u_texture, v_texCoords + vec2( u_offset, -u_offset) * u_pixelSize);
    sum += texture2D(u_texture, v_texCoords + vec2(-u_offset,  u_offset) * u_pixelSize);
    sum += texture2D(u_texture, v_texCoords + vec2( u_offset,  u_offset) * u_pixelSize);
    gl_FragColor = sum * 0.25;
}
