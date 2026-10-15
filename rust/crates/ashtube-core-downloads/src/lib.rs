use ashtube_core_models::CoreError;

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct DownloadSelection {
    pub media_id: String,
    pub quality: String,
    pub source_is_authorized: bool,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct DownloadPlan {
    pub media_id: String,
    pub quality: String,
    pub state: DownloadPlanState,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum DownloadPlanState {
    AwaitingAndroidDestination,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum DownloadStatus {
    Queued,
    Downloading { bytes_downloaded: u64, total_bytes: u64 },
    Paused,
    Completed,
    Failed(String),
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct DownloadRecord {
    pub id: String,
    pub media_id: String,
    pub quality: String,
    pub status: DownloadStatus,
}

impl DownloadRecord {
    pub fn new(id: impl Into<String>, media_id: impl Into<String>, quality: impl Into<String>) -> Self {
        Self {
            id: id.into(),
            media_id: media_id.into(),
            quality: quality.into(),
            status: DownloadStatus::Queued,
        }
    }

    pub fn pause(&mut self) -> Result<(), CoreError> {
        match self.status {
            DownloadStatus::Downloading { .. } | DownloadStatus::Queued => {
                self.status = DownloadStatus::Paused;
                Ok(())
            }
            _ => Err(CoreError::InvalidRequest("Cannot pause a download that is not active.".into())),
        }
    }

    pub fn resume(&mut self) -> Result<(), CoreError> {
        match self.status {
            DownloadStatus::Paused | DownloadStatus::Failed(_) => {
                self.status = DownloadStatus::Downloading {
                    bytes_downloaded: 0,
                    total_bytes: 100_000_000,
                };
                Ok(())
            }
            _ => Err(CoreError::InvalidRequest("Cannot resume an active or completed download.".into())),
        }
    }
}

pub fn create_download_plan(selection: DownloadSelection) -> Result<DownloadPlan, CoreError> {
    if selection.media_id.trim().is_empty() {
        return Err(CoreError::InvalidRequest("media ID cannot be empty".into()));
    }
    if selection.quality.trim().is_empty() {
        return Err(CoreError::InvalidRequest("quality cannot be empty".into()));
    }
    if !selection.source_is_authorized {
        return Err(CoreError::UnauthorizedSource);
    }
    Ok(DownloadPlan {
        media_id: selection.media_id,
        quality: selection.quality,
        state: DownloadPlanState::AwaitingAndroidDestination,
    })
}

#[cfg(test)]
mod tests {
    use super::{create_download_plan, DownloadSelection};
    use ashtube_core_models::CoreError;

    #[test]
    fn unauthorized_sources_cannot_create_download_plans() {
        let error = create_download_plan(DownloadSelection {
            media_id: "mock-movie-1".into(),
            quality: "1080p".into(),
            source_is_authorized: false,
        })
        .unwrap_err();
        assert_eq!(error, CoreError::UnauthorizedSource);
    }

    #[test]
    fn download_record_lifecycle_pause_and_resume() {
        use super::{DownloadRecord, DownloadStatus};
        let mut record = DownloadRecord::new("dl-1", "mock-movie-1", "1080p");
        assert_eq!(record.status, DownloadStatus::Queued);

        record.pause().unwrap();
        assert_eq!(record.status, DownloadStatus::Paused);

        record.resume().unwrap();
        match record.status {
            DownloadStatus::Downloading { total_bytes, .. } => assert_eq!(total_bytes, 100_000_000),
            _ => panic!("Expected Downloading status"),
        }
    }
}
