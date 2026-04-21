param(
    [string] $ApiBaseUrl = "http://localhost:8081",
    [string] $CustomerId = "customer-a",
    [string] $CampaignId = "softphone-console",
    [string] $AgentId = "1001",
    [string] $Destination = "+15551234567",
    [string] $SipDomain = "asterisk.example.com",
    [string] $WebSocketUrl = "wss://asterisk.example.com:8089/ws",
    [switch] $PlanOnly
)

$ErrorActionPreference = "Stop"

function Join-Url {
    param(
        [string] $BaseUrl,
        [string] $Path
    )

    return $BaseUrl.TrimEnd("/") + "/" + $Path.TrimStart("/")
}

function Invoke-JsonRequest {
    param(
        [string] $Method,
        [string] $Path,
        [object] $Body = $null
    )

    $uri = Join-Url -BaseUrl $ApiBaseUrl -Path $Path
    if ($PlanOnly) {
        Write-Host "PLAN $Method $uri"
        if ($null -ne $Body) {
            $Body | ConvertTo-Json -Depth 8
        }
        return $null
    }

    if ($null -eq $Body) {
        return Invoke-RestMethod -Method $Method -Uri $uri
    }

    return Invoke-RestMethod `
        -Method $Method `
        -Uri $uri `
        -ContentType "application/json" `
        -Body ($Body | ConvertTo-Json -Depth 8)
}

function New-WrapUpPayload {
    param(
        [string] $FollowUpAt
    )

    return @{
        campaignId = $CampaignId
        provider = "SOFTPHONE"
        customerNumber = $Destination
        agentId = $AgentId
        callMode = "AGENT_SOFTPHONE"
        callDirection = "outgoing"
        callStatus = "ended"
        disposition = "callback"
        notes = "Smoke test wrap-up sync"
        priority = "high"
        followUpAt = $FollowUpAt
    }
}

function Assert-Equal {
    param(
        [string] $Label,
        [object] $Expected,
        [object] $Actual
    )

    if ($Actual -ne $Expected) {
        throw "Expected $Label $Expected, got $Actual"
    }
}

function Assert-NotBlank {
    param(
        [string] $Label,
        [object] $Actual
    )

    if ([string]::IsNullOrWhiteSpace([string] $Actual)) {
        throw "Expected $Label to be present"
    }
}

Write-Host "Softphone integration smoke"
Write-Host "API: $ApiBaseUrl"
Write-Host "Customer: $CustomerId"
Write-Host "Agent: $AgentId"
Write-Host "Campaign: $CampaignId"
Write-Host ""

$customerPayload = @{
    customerId = $CustomerId
    customerName = "Smoke Customer"
    sipDomain = $SipDomain
    webSocketUrl = $WebSocketUrl
    apiBaseUrl = $ApiBaseUrl
    defaultAgentUiMode = "jssip"
    proposalPreset = "WEBRTC_CONTACT_CENTER"
    brandDisplayName = "Smoke Contact Center"
}

Invoke-JsonRequest -Method "POST" -Path "customers" -Body $customerPayload | Out-Null

$agentPayload = @{
    agentId = $AgentId
    agentName = "Smoke Agent $AgentId"
    extensionNumber = $AgentId
    sipUsername = $AgentId
    sipPassword = "StrongPassword$AgentId"
}

Invoke-JsonRequest -Method "POST" -Path "agents" -Body $agentPayload | Out-Null
Invoke-JsonRequest -Method "POST" -Path "agents/$AgentId/available" | Out-Null

$queuePayload = @{
    customerNumber = $Destination
    campaignId = $CampaignId
    agentId = $AgentId
    agentChannel = "PJSIP/$AgentId"
    provider = "ASTERISK"
    callMode = "AGENT_ASSISTED"
}

$queued = Invoke-JsonRequest -Method "POST" -Path "outbound/start" -Body $queuePayload

if ($PlanOnly) {
    $plannedWrapUpPayload = New-WrapUpPayload -FollowUpAt "<utc-iso-follow-up>"

    Write-Host ""
    Write-Host "PLAN poll GET $(Join-Url -BaseUrl $ApiBaseUrl -Path 'outbound/sessions/<callSessionId>')"
    Write-Host "PLAN save PUT $(Join-Url -BaseUrl $ApiBaseUrl -Path 'outbound/sessions/<callSessionId>/wrap-up')"
    $plannedWrapUpPayload | ConvertTo-Json -Depth 8
    Write-Host ""
    Write-Host "Plan complete. Run without -PlanOnly once dialer-api is listening."
    exit 0
}

$callSessionId = $queued.callSessionId
if ([string]::IsNullOrWhiteSpace($callSessionId)) {
    throw "Outbound queue did not return callSessionId"
}

Write-Host "Queued call session: $callSessionId"

$session = $null
for ($attempt = 1; $attempt -le 5; $attempt++) {
    $session = Invoke-JsonRequest -Method "GET" -Path "outbound/sessions/$callSessionId"
    Write-Host ("Poll {0}: status={1} event={2}" -f $attempt, $session.status, $session.lastEventType)
    Start-Sleep -Seconds 1
}

$wrapUpPayload = New-WrapUpPayload -FollowUpAt (Get-Date).ToUniversalTime().AddHours(1).ToString("o")

$wrapUp = Invoke-JsonRequest -Method "PUT" -Path "outbound/sessions/$callSessionId/wrap-up" -Body $wrapUpPayload
$verified = Invoke-JsonRequest -Method "GET" -Path "outbound/sessions/$callSessionId"

Assert-Equal -Label "wrap-up response campaignId" -Expected $CampaignId -Actual $wrapUp.campaignId
Assert-Equal -Label "wrap-up response callSessionId" -Expected $callSessionId -Actual $wrapUp.callSessionId
Assert-Equal -Label "wrap-up response agentId" -Expected $AgentId -Actual $wrapUp.agentId
Assert-Equal -Label "wrap-up response disposition" -Expected "callback" -Actual $wrapUp.disposition
Assert-Equal -Label "wrap-up response notes" -Expected "Smoke test wrap-up sync" -Actual $wrapUp.notes
Assert-Equal -Label "wrap-up response priority" -Expected "high" -Actual $wrapUp.priority
Assert-NotBlank -Label "wrap-up response followUpAt" -Actual $wrapUp.followUpAt
Assert-NotBlank -Label "wrap-up response wrapUpUpdatedAt" -Actual $wrapUp.wrapUpUpdatedAt
Assert-Equal -Label "verified session campaignId" -Expected $CampaignId -Actual $verified.campaignId
Assert-Equal -Label "verified session operatorDisposition" -Expected "callback" -Actual $verified.operatorDisposition
Assert-Equal -Label "verified session operatorNotes" -Expected "Smoke test wrap-up sync" -Actual $verified.operatorNotes
Assert-Equal -Label "verified session operatorPriority" -Expected "high" -Actual $verified.operatorPriority
Assert-NotBlank -Label "verified session followUpAt" -Actual $verified.followUpAt
Assert-NotBlank -Label "verified session wrapUpUpdatedAt" -Actual $verified.wrapUpUpdatedAt

Write-Host "Wrap-up verified for session: $callSessionId"
Write-Host "Smoke complete."
