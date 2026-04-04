#ifdef GL_ES
precision mediump float;
#endif
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform float u_width;

void main() {
    float x = 1.0 / u_width;
    vec4 sum = vec4(0.0);
    sum += texture2D(u_texture, vec2(v_texCoords.x - 4.0*x, v_texCoords.y)) * 0.0162;
    sum += texture2D(u_texture, vec2(v_texCoords.x - 3.0*x, v_texCoords.y)) * 0.0540;
    sum += texture2D(u_texture, vec2(v_texCoords.x - 2.0*x, v_texCoords.y)) * 0.1216;
    sum += texture2D(u_texture, vec2(v_texCoords.x - 1.0*x, v_texCoords.y)) * 0.1945;
    sum += texture2D(u_texture, v_texCoords)                                 * 0.2275;
    sum += texture2D(u_texture, vec2(v_texCoords.x + 1.0*x, v_texCoords.y)) * 0.1945;
    sum += texture2D(u_texture, vec2(v_texCoords.x + 2.0*x, v_texCoords.y)) * 0.1216;
    sum += texture2D(u_texture, vec2(v_texCoords.x + 3.0*x, v_texCoords.y)) * 0.0540;
    sum += texture2D(u_texture, vec2(v_texCoords.x + 4.0*x, v_texCoords.y)) * 0.0162;
    gl_FragColor = sum;
}
