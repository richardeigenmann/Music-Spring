import { Routes } from '@angular/router';
import { Playlists } from './playlists/playlists';
import { TracksByGroup } from './tracks-by-group/tracks-by-group';
import { TrackEdit } from './track-edit/track-edit';
import { TrackPlayer } from './track-player/track-player';
import { UnclassifiedTracks } from './unclassified-tracks/unclassified-tracks';

export const routes: Routes = [
    { path: 'playlists', component: Playlists },
    { path: 'group/:groupId', component: TracksByGroup },
    { path: 'track/:id', component: TrackEdit },
    { path: 'player', component: TrackPlayer },
    { path: 'unclassified', component: UnclassifiedTracks },
    { path: '', redirectTo: '/playlists', pathMatch: 'full' }
];
