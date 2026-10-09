{
  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixos-25.11";
    flake-parts.url = "github:hercules-ci/flake-parts";
    systems.url = "github:nix-systems/default";
  };

  outputs = inputs:
    inputs.flake-parts.lib.mkFlake { inherit inputs; } {
      systems = import inputs.systems;

      perSystem = { config, self', pkgs, lib, system, ... }: let
        mc-java = pkgs.openjdk25;

        nativeBuildInputs = with pkgs; [
          mc-java
          git
        ];

        buildInputs = with pkgs; [
          libGL
          vulkan-loader
          vulkan-volk
          vulkan-tools
          vulkan-headers
          wayland
          libdecor
          libpulseaudio
          alsa-lib
          flite
        ];
      in {
        devShells.default = pkgs.mkShell {
          inherit nativeBuildInputs buildInputs;

          env = {
            LD_LIBRARY_PATH = "${lib.makeLibraryPath buildInputs}:/run/opengl-driver/lib";
            JAVA_HOME = "${mc-java.home}";
          };
        };
      };
    };
}