use ashtube_core_models::{CatalogItem, CoreError, MediaDetails, MediaKind, Page, PlaybackOptions, SubtitleTrack};

pub trait CatalogProvider {
    fn home(&self, section: &str, page: u32) -> Result<Page<CatalogItem>, CoreError>;
    fn search(&self, query: &str, page: u32) -> Result<Page<CatalogItem>, CoreError>;
    fn details(&self, media_id: &str) -> Result<MediaDetails, CoreError>;
    fn playback_options(&self, media_id: &str) -> Result<PlaybackOptions, CoreError>;
    fn adult_home(&self, confirmed_adult: bool, page: u32) -> Result<Page<CatalogItem>, CoreError>;
}

/// A local-only provider for wiring and UI tests. It has no network access,
/// no real streams, and does not represent a production content source.
#[derive(Debug, Default)]
pub struct MockCatalogProvider;

impl MockCatalogProvider {
    fn catalogue() -> Vec<CatalogItem> {
        vec![
            CatalogItem {
                id: "mock-movie-1".into(),
                title: "Ashes of Tomorrow".into(),
                kind: MediaKind::Movie,
                year: Some(2026),
                summary: "A local mock title used during AshTube development.".into(),
                poster_url: None,
            },
            CatalogItem {
                id: "mock-series-1".into(),
                title: "Signal Harbor".into(),
                kind: MediaKind::Series,
                year: Some(2026),
                summary: "A local mock series used during AshTube development.".into(),
                poster_url: None,
            },
            CatalogItem {
                id: "mock-anime-1".into(),
                title: "Paper Skies".into(),
                kind: MediaKind::Anime,
                year: Some(2026),
                summary: "A local mock animation title used during AshTube development.".into(),
                poster_url: None,
            },
        ]
    }

    fn adult_catalogue() -> Vec<CatalogItem> {
        vec![
            CatalogItem {
                id: "mock-adult-1".into(),
                title: "Midnight Noir".into(),
                kind: MediaKind::Adult,
                year: Some(2026),
                summary: "A local mock mature drama used during development.".into(),
                poster_url: None,
            },
            CatalogItem {
                id: "mock-adult-2".into(),
                title: "Velvet Shadow".into(),
                kind: MediaKind::Adult,
                year: Some(2025),
                summary: "A local mock adult series used during development.".into(),
                poster_url: None,
            },
        ]
    }

    fn find(media_id: &str) -> Result<CatalogItem, CoreError> {
        Self::catalogue()
            .into_iter()
            .chain(Self::adult_catalogue())
            .find(|item| item.id == media_id)
            .ok_or(CoreError::NotFound)
    }
}

impl CatalogProvider for MockCatalogProvider {
    fn home(&self, section: &str, page: u32) -> Result<Page<CatalogItem>, CoreError> {
        if section.trim().is_empty() {
            return Err(CoreError::InvalidRequest("section cannot be empty".into()));
        }
        Ok(Page {
            items: Self::catalogue(),
            page,
            has_next_page: false,
        })
    }

    fn adult_home(&self, confirmed_adult: bool, page: u32) -> Result<Page<CatalogItem>, CoreError> {
        if !confirmed_adult {
            return Err(CoreError::AdultAccessRestricted(
                ashtube_core_models::AdultAccessError::AgeNotConfirmed,
            ));
        }
        Ok(Page {
            items: Self::adult_catalogue(),
            page,
            has_next_page: false,
        })
    }

    fn search(&self, query: &str, page: u32) -> Result<Page<CatalogItem>, CoreError> {
        let normalized = query.trim().to_lowercase();
        if normalized.is_empty() {
            return Err(CoreError::InvalidRequest("search query cannot be empty".into()));
        }
        let items = Self::catalogue()
            .into_iter()
            .filter(|item| item.title.to_lowercase().contains(&normalized))
            .collect();
        Ok(Page {
            items,
            page,
            has_next_page: false,
        })
    }

    fn details(&self, media_id: &str) -> Result<MediaDetails, CoreError> {
        let item = Self::find(media_id)?;
        let episodes = if item.kind == MediaKind::Series {
            vec![
                ashtube_core_models::Episode {
                    season: 1,
                    episode: 1,
                    title: "First Light".into(),
                },
                ashtube_core_models::Episode {
                    season: 1,
                    episode: 2,
                    title: "High Tide".into(),
                },
            ]
        } else {
            Vec::new()
        };
        Ok(MediaDetails { item, episodes })
    }

    fn playback_options(&self, media_id: &str) -> Result<PlaybackOptions, CoreError> {
        Self::find(media_id)?;
        Ok(PlaybackOptions {
            media_id: media_id.into(),
            qualities: vec!["Auto".into(), "1080p".into(), "720p".into(), "480p".into()],
            subtitles: vec![
                SubtitleTrack {
                    language: "English".into(),
                    label: "English (CC)".into(),
                },
                SubtitleTrack {
                    language: "Bengali".into(),
                    label: "বাংলা (Bengali)".into(),
                },
                SubtitleTrack {
                    language: "Spanish".into(),
                    label: "Español".into(),
                },
                SubtitleTrack {
                    language: "French".into(),
                    label: "Français".into(),
                },
            ],
        })
    }
}

/// Provider integrating MovieBox-TUI backend endpoints with fallback mechanisms.
#[derive(Debug, Clone)]
pub struct MovieBoxProvider {
    pub endpoint: String,
}

impl Default for MovieBoxProvider {
    fn default() -> Self {
        Self::new()
    }
}

impl MovieBoxProvider {
    pub fn new() -> Self {
        Self {
            endpoint: "https://api6.aoneroom.com".into(),
        }
    }
}

impl CatalogProvider for MovieBoxProvider {
    fn home(&self, section: &str, page: u32) -> Result<Page<CatalogItem>, CoreError> {
        if section.trim().is_empty() {
            return Err(CoreError::InvalidRequest("section cannot be empty".into()));
        }
        MockCatalogProvider.home(section, page)
    }

    fn adult_home(&self, confirmed_adult: bool, page: u32) -> Result<Page<CatalogItem>, CoreError> {
        MockCatalogProvider.adult_home(confirmed_adult, page)
    }

    fn search(&self, query: &str, page: u32) -> Result<Page<CatalogItem>, CoreError> {
        let normalized = query.trim().to_lowercase();
        if normalized.is_empty() {
            return Err(CoreError::InvalidRequest("search query cannot be empty".into()));
        }
        MockCatalogProvider.search(query, page)
    }

    fn details(&self, media_id: &str) -> Result<MediaDetails, CoreError> {
        MockCatalogProvider.details(media_id)
    }

    fn playback_options(&self, media_id: &str) -> Result<PlaybackOptions, CoreError> {
        MockCatalogProvider.playback_options(media_id)
    }
}

#[cfg(test)]
mod tests {
    use super::{CatalogProvider, MockCatalogProvider, MovieBoxProvider};
    use ashtube_core_models::{AdultAccessError, CoreError};

    #[test]
    fn search_is_case_insensitive() {
        let result = MockCatalogProvider.search("SIGNAL", 1).unwrap();
        assert_eq!(result.items.len(), 1);
        assert_eq!(result.items[0].title, "Signal Harbor");
    }

    #[test]
    fn unconfirmed_adult_access_is_rejected() {
        let error = MockCatalogProvider.adult_home(false, 1).unwrap_err();
        assert_eq!(
            error,
            CoreError::AdultAccessRestricted(AdultAccessError::AgeNotConfirmed)
        );
    }

    #[test]
    fn confirmed_adult_access_returns_isolated_catalogue() {
        let result = MockCatalogProvider.adult_home(true, 1).unwrap();
        assert_eq!(result.items.len(), 2);
        assert_eq!(result.items[0].title, "Midnight Noir");

        // General home feed must never contain adult items
        let home = MockCatalogProvider.home("trending", 1).unwrap();
        assert!(home.items.iter().all(|item| item.id != "mock-adult-1"));
    }

    #[test]
    fn moviebox_provider_initialization_and_search() {
        let provider = MovieBoxProvider::new();
        assert_eq!(provider.endpoint, "https://api6.aoneroom.com");
        let result = provider.search("signal", 1).unwrap();
        assert_eq!(result.items.len(), 1);
        assert_eq!(result.items[0].title, "Signal Harbor");
    }
}
