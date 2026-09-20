<#
.SYNOPSIS
    Triggers a Release workflow test build on GitHub Actions - builds the Windows/Linux/macOS
    app images without publishing anything, so you can download and check them from the run.

.DESCRIPTION
    Runs the "Release" workflow (.github/workflows/release.yml) via workflow_dispatch with the
    "release_tag" input left empty, which is that workflow's documented trigger for a plain test
    build: the build matrix runs on all three platforms and uploads the resulting archives as
    workflow-run artifacts, but the "release" job is skipped, so no GitHub Release is created or
    updated and no git tag is touched.

    Requires the GitHub CLI ("gh"), authenticated against the repo (run "gh auth login" once if
    needed).

.PARAMETER Branch
    Branch to build from. Defaults to the current branch of the local repo.

.EXAMPLE
    .\build\run-release-test-build.ps1

.EXAMPLE
    .\build\run-release-test-build.ps1 -Branch feature/foo
#>
[CmdletBinding()]
param(
    [string]$Branch
)

# This script lives at <git repo root>/com.wudsn.productions.atari800.thecartstudio/build, two
# levels below the git repo root where .github/workflows/release.yml is.
$RepoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)

if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    throw "GitHub CLI ('gh') not found on PATH. Install it from https://cli.github.com/ and run 'gh auth login' first."
}

if (-not $Branch) {
    $Branch = (git -C $RepoRoot rev-parse --abbrev-ref HEAD).Trim()
}

$RemoteUrl = (git -C $RepoRoot remote get-url origin).Trim()
if ($RemoteUrl -notmatch 'github\.com[:/](?<repo>[^/]+/[^/]+?)(\.git)?$') {
    throw "Could not determine GitHub repo from origin remote URL: $RemoteUrl"
}
$Repo = $Matches['repo']

Write-Host "Triggering Release workflow test build on '$Repo' branch '$Branch'..."
gh workflow run release.yml --repo $Repo --ref $Branch
if ($LASTEXITCODE -ne 0) {
    throw "Failed to trigger the workflow (gh exited with code $LASTEXITCODE)."
}

Write-Host "Triggered. Track progress with: gh run watch --repo $Repo (or check the Actions tab)."
