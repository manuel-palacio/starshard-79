#ifdef GL_ES
precision mediump float;
#endif
varying vec2 v_texCoords;
uniform sampler2D u_texture;

// Extract pixels brighter than this luminance threshold.
// 0.55 keeps the glow tight to the vector lines without haloing dim geometry.
const float THRESHOLD = 0.55;

void main() {
    vec4 color = texture2D(u_texture, v_texCoords);
    float lum  = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
    gl_FragColor = (lum > THRESHOLD) ? color : vec4(0.0);
}
