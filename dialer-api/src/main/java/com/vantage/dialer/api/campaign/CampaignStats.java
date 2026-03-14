package com.vantage.dialer.api.campaign;

public class CampaignStats {
    private long total;
    private long newCount;
    private long queued;
    private long inProgress;
    private long completed;
    private long failed;

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public long getNewCount() { return newCount; }
    public void setNewCount(long newCount) { this.newCount = newCount; }

    public long getQueued() { return queued; }
    public void setQueued(long queued) { this.queued = queued; }

    public long getInProgress() { return inProgress; }
    public void setInProgress(long inProgress) { this.inProgress = inProgress; }

    public long getCompleted() { return completed; }
    public void setCompleted(long completed) { this.completed = completed; }

    public long getFailed() { return failed; }
    public void setFailed(long failed) { this.failed = failed; }
}