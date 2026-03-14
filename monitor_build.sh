#!/bin/bash

if [ -z "$GH_TOKEN" ]; then
    echo "Error: GH_TOKEN environment variable not set"
    exit 1
fi

REPO="UniverseKing4/discord-android-client"

echo "Monitoring workflow..."

while true; do
    STATUS=$(gh run list --repo "$REPO" --limit 1 --json status --jq '.[0].status')
    
    if [[ "$STATUS" == "completed" ]]; then
        CONCLUSION=$(gh run list --repo "$REPO" --limit 1 --json conclusion --jq '.[0].conclusion')
        
        if [[ "$CONCLUSION" == "success" ]]; then
            echo "Build successful!"
            echo "Downloading APK..."
            
            LATEST_TAG=$(gh release list --repo "$REPO" --limit 1 --json tagName --jq '.[0].tagName')
            gh release download "$LATEST_TAG" --repo "$REPO" --pattern "*.apk" --dir /storage/emulated/0/Download 2>/dev/null || \
            gh release download "$LATEST_TAG" --repo "$REPO" --pattern "*.apk" --dir ~/Download 2>/dev/null || \
            gh release download "$LATEST_TAG" --repo "$REPO" --pattern "*.apk"
            
            echo "APK downloaded successfully!"
            break
        else
            echo "Build failed with conclusion: $CONCLUSION"
            echo "Fetching logs..."
            gh run view --repo "$REPO" --log-failed
            break
        fi
    elif [[ "$STATUS" == "in_progress" ]] || [[ "$STATUS" == "queued" ]]; then
        echo "Build status: $STATUS - waiting..."
        sleep 10
    else
        echo "Unexpected status: $STATUS"
        break
    fi
done
