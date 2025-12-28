#!/bin/bash
# rebuild_models.sh
# Script to recombine split ONNX model files from chunks

set -e  # Stop on any error

# Navigate to assets folder
ASSETS_DIR="app/src/main/assets"
cd "$ASSETS_DIR" || { echo "Directory $ASSETS_DIR not found"; exit 1; }

echo "Rebuilding ONNX model files from chunks..."

# Array of base filenames (without extensions)
FILES=(
  "NLLB_decoder"
  "NLLB_embed_and_lm_head"
  "NLLB_encoder"
  "Whisper_decoder"
  "Whisper_encoder"
)

for file in "${FILES[@]}"; do
    # Check if chunks exist
    if ls "${file}.part_"* 1> /dev/null 2>&1; then
        echo "Rebuilding $file.onnx..."
        cat "${file}.part_"* > "${file}.onnx"
        echo "$file.onnx rebuilt successfully."
    else
        echo "No chunks found for $file, skipping."
    fi
done

echo "All files processed!"
