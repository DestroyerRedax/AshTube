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
}
