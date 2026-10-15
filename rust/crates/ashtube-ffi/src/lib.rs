use ashtube_core_downloads::{create_download_plan, DownloadPlan, DownloadSelection};
use ashtube_core_models::{CatalogItem, CoreError, MediaDetails, Page, PlaybackOptions};
use ashtube_core_providers::{CatalogProvider, MockCatalogProvider};

/// Kotlin/UniFFI integration will wrap this facade after the Rust toolchain and
/// approved-provider registry are in place. It currently exposes mock-only data.
#[derive(Debug, Default)]
pub struct AshTubeCore {
    provider: MockCatalogProvider,
}

impl AshTubeCore {
    pub fn home(&self, section: &str, page: u32) -> Result<Page<CatalogItem>, CoreError> {
        self.provider.home(section, page)
    }

    pub fn search(&self, query: &str, page: u32) -> Result<Page<CatalogItem>, CoreError> {
        self.provider.search(query, page)
    }

    pub fn details(&self, media_id: &str) -> Result<MediaDetails, CoreError> {
        self.provider.details(media_id)
    }

    pub fn playback_options(&self, media_id: &str) -> Result<PlaybackOptions, CoreError> {
        self.provider.playback_options(media_id)
    }

    pub fn adult_home(
        &self,
        confirmed_adult: bool,
        page: u32,
    ) -> Result<Page<CatalogItem>, CoreError> {
        self.provider.adult_home(confirmed_adult, page)
    }

    pub fn create_download_plan(
        &self,
        selection: DownloadSelection,
    ) -> Result<DownloadPlan, CoreError> {
        create_download_plan(selection)
    }
}

#[cfg(test)]
mod tests {
    use super::AshTubeCore;

    #[test]
    fn facade_returns_mock_home_content() {
        let core = AshTubeCore::default();
        let home = core.home("trending", 1).unwrap();
        assert_eq!(home.items.len(), 3);
    }

    #[test]
    fn facade_returns_mock_details_and_playback_options() {
        let core = AshTubeCore::default();
        let details = core.details("mock-series-1").unwrap();
        assert_eq!(details.item.title, "Signal Harbor");
        assert_eq!(details.episodes.len(), 2);

        let options = core.playback_options("mock-series-1").unwrap();
        assert_eq!(options.qualities, vec!["Auto", "1080p", "720p", "480p"]);
        assert_eq!(options.subtitles.len(), 4);
    }

    #[test]
    fn facade_protects_adult_content() {
        let core = AshTubeCore::default();
        assert!(core.adult_home(false, 1).is_err());
        let adult = core.adult_home(true, 1).unwrap();
        assert_eq!(adult.items.len(), 2);
    }
}
