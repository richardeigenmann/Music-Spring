import { Routes } from '@angular/router';
import { Playlists } from './playlists/playlists';
import { TracksByGroup } from './tracks-by-group/tracks-by-group';

export const routes: Routes = [
    { path: 'playlists', component: Playlists },
    { path: 'group/:groupId', component: TracksByGroup },
    { path: '', redirectTo: '/playlists', pathMatch: 'full' }
];
