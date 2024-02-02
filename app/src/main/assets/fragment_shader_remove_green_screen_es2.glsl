#version 100

// Fragment shader that maps green pixels to 0 alpha, otherwise leve pixels unchanged
precision mediump float;
uniform sampler2D uTexSampler;
uniform vec3 uColor;
varying vec2 vTexSamplingCoord;

void main() {
    vec3 src = texture2D(uTexSampler, vTexSamplingCoord).rgb;
    float diff = length(src - uColor.rgb);
    float alpha = 1.0;
    if (diff < 0.3) {
        alpha = 0.0;
    }
    gl_FragColor = vec4(src, alpha);
}
