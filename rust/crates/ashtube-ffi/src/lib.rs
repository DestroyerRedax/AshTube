use ashtube_core_downloads::{create_download_plan, DownloadPlan, DownloadSelection};
use ashtube_core_models::{CatalogItem, CoreError, FavoriteItem, HistoryItem, MediaDetails, Page, PlaybackOptions};
use ashtube_core_providers::{CatalogProvider, MockCatalogProvider};
use ashtube_core_storage::{MockStorage, Storage};

/// Kotlin/UniFFI integration will wrap this facade after the Rust toolchain and
/// approved-provider registry are in place. It currently exposes mock-only data.
#[derive(Debug, Default)]
pub struct AshTubeCore {
    provider: MockCatalogProvider,
    storage: MockStorage,
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

    pub fn is_favorite(&self, media_id: &str) -> Result<bool, CoreError> {
        self.storage.is_favorite(media_id)
    }

    pub fn set_favorite(&mut self, item: FavoriteItem, is_favorite: bool) -> Result<(), CoreError> {
        self.storage.set_favorite(item, is_favorite)
    }

    pub fn get_favorites(&self) -> Result<Vec<FavoriteItem>, CoreError> {
        self.storage.get_favorites()
    }

    pub fn record_history(&mut self, item: HistoryItem) -> Result<(), CoreError> {
        self.storage.record_history(item)
    }

    pub fn get_history(&self) -> Result<Vec<HistoryItem>, CoreError> {
        self.storage.get_history()
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

    #[test]
    fn facade_manages_storage_contracts() {
        let mut core = AshTubeCore::default();
        let item = FavoriteItem {
            media_id: "mock-movie-1".into(),
            title: "Ashes of Tomorrow".into(),
        };

        assert!(!core.is_favorite("mock-movie-1").unwrap());
        core.set_favorite(item, true).unwrap();
        assert!(core.is_favorite("mock-movie-1").unwrap());
        assert_eq!(core.get_favorites().unwrap().len(), 1);

        let history_item = HistoryItem {
            media_id: "mock-movie-1".into(),
            title: "Ashes of Tomorrow".into(),
            position_ms: 60_000,
            duration_ms: 7_200_000,
        };
        core.record_history(history_item).unwrap();
        assert_eq!(core.get_history().unwrap().len(), 1);
    }
}
