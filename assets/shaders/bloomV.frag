#ifdef GL_ES
precision mediump float;
#endif
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform float u_height;

void main() {
    float y = 1.0 / u_height;
    vec4 sum = vec4(0.0);
    sum += texture2D(u_texture, vec2(v_texCoords.x, v_texCoords.y - 4.0*y)) * 0.0162;
    sum += texture2D(u_texture, vec2(v_texCoords.x, v_texCoords.y - 3.0*y)) * 0.0540;
    sum += texture2D(u_texture, vec2(v_texCoords.x, v_texCoords.y - 2.0*y)) * 0.1216;
    sum += texture2D(u_texture, vec2(v_texCoords.x, v_texCoords.y - 1.0*y)) * 0.1945;
    sum += texture2D(u_texture, v_texCoords)                                  * 0.2275;
    sum += texture2D(u_texture, vec2(v_texCoords.x, v_texCoords.y + 1.0*y)) * 0.1945;
    sum += texture2D(u_texture, vec2(v_texCoords.x, v_texCoords.y + 2.0*y)) * 0.1216;
    sum += texture2D(u_texture, vec2(v_texCoords.x, v_texCoords.y + 3.0*y)) * 0.0540;
    sum += texture2D(u_texture, vec2(v_texCoords.x, v_texCoords.y + 4.0*y)) * 0.0162;
    gl_FragColor = sum;
}
