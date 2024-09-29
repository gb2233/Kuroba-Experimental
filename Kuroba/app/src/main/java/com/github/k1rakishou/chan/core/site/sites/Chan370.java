/*
 * KurobaEx - *chan browser https://github.com/K1rakishou/Kuroba-Experimental/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.k1rakishou.chan.core.site.sites;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.k1rakishou.chan.core.site.ChunkDownloaderSiteProperties;
import com.github.k1rakishou.chan.core.site.Site;
import com.github.k1rakishou.chan.core.site.SiteIcon;
import com.github.k1rakishou.chan.core.site.common.CommonSite;
import com.github.k1rakishou.chan.core.site.common.vichan.VichanActions;
import com.github.k1rakishou.chan.core.site.common.vichan.VichanApi;
import com.github.k1rakishou.chan.core.site.common.vichan.VichanCommentParser;
import com.github.k1rakishou.chan.core.site.common.vichan.VichanEndpoints;
import com.github.k1rakishou.chan.core.site.parser.CommentParserType;
import com.github.k1rakishou.common.DoNotStrip;
import com.github.k1rakishou.model.data.board.ChanBoard;
import com.github.k1rakishou.model.data.descriptor.BoardDescriptor;
import com.github.k1rakishou.model.data.descriptor.ChanDescriptor;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

import okhttp3.HttpUrl;

@DoNotStrip
public class Chan370
        extends CommonSite {
    private final ChunkDownloaderSiteProperties chunkDownloaderSiteProperties;
    public static final String SITE_NAME = "370chan";

    public static final CommonSiteUrlHandler URL_HANDLER = new CommonSiteUrlHandler() {
        private static final String ROOT = "https://370ch.lt/";

        @Override
        public Class<? extends Site> getSiteClass() {
            return Chan370.class;
        }

        @Override
        public HttpUrl getUrl() {
            return HttpUrl.parse(ROOT);
        }

        @Override
        public HttpUrl[] getMediaHosts() {
            return new HttpUrl[]{getUrl()};
        }

        @Override
        public String[] getNames() {
            return new String[]{"370chan"};
        }

        @Override
        public String desktopUrl(ChanDescriptor chanDescriptor, @Nullable Long postNo) {
            if (chanDescriptor instanceof ChanDescriptor.CatalogDescriptor) {
                return getUrl().newBuilder()
                        .addPathSegment(chanDescriptor.boardCode())
                        .toString();
            } else if (chanDescriptor instanceof ChanDescriptor.ThreadDescriptor) {
                return getUrl().newBuilder()
                        .addPathSegment(chanDescriptor.boardCode())
                        .addPathSegment("res")
                        .addPathSegment(((ChanDescriptor.ThreadDescriptor) chanDescriptor).getThreadNo() + ".html")
                        .toString();
            } else {
                return null;
            }
        }
    };

    public Chan370() {
        chunkDownloaderSiteProperties = new ChunkDownloaderSiteProperties(true, true);
    }

    @Override
    public void setup() {
        setEnabled(true);
        setName(SITE_NAME);
        setIcon(SiteIcon.fromFavicon(getImageLoaderDeprecatedLazy(), HttpUrl.parse("https://370ch.lt/favicon.ico")));

        setBoards(
                ChanBoard.create(BoardDescriptor.create(siteDescriptor().getSiteName(), "a"), "anime ir manga"),
                ChanBoard.create(BoardDescriptor.create(siteDescriptor().getSiteName(), "b"), "apie viską"),
                ChanBoard.create(BoardDescriptor.create(siteDescriptor().getSiteName(), "g"), "technologijos ir žaidimai"),
                ChanBoard.create(BoardDescriptor.create(siteDescriptor().getSiteName(), "fo"), "fotografija"),
                ChanBoard.create(BoardDescriptor.create(siteDescriptor().getSiteName(), "mu"), "muzika"),
                ChanBoard.create(BoardDescriptor.create(siteDescriptor().getSiteName(), "int"), "internacionalus"),
                ChanBoard.create(BoardDescriptor.create(siteDescriptor().getSiteName(), "t"), "teptukas"),
                ChanBoard.create(BoardDescriptor.create(siteDescriptor().getSiteName(), "meta"), "svetainės aptarimas")
        );

        setResolvable(URL_HANDLER);

        setConfig(new CommonConfig() {
            @Override
            public boolean siteFeature(SiteFeature siteFeature) {
                return super.siteFeature(siteFeature); //features are not implemented.
            }
        });

        setEndpoints(new VichanEndpoints(this, "https://370ch.lt/", "https://370ch.lt/")
        {
            @Override
            public HttpUrl thumbnailUrl(BoardDescriptor boardDescriptor, boolean spoiler, int customSpoilers, Map<String, String> arg) {
                String extension = switch (arg.get("ext")){
                    // for an unknown reason, not all media files follow the same rules
                    // i.e. some jpg images have png thumbnails, others have jpg
                    // this makes some amount of media files have 404 thumbnails
                    case "jpg", "jpeg" -> "." + arg.get("ext");
                    case "webm", "mp4", "gif" -> ".gif";
                    default -> ".png";
                };
                return root.builder()
                        .s(boardDescriptor.getBoardCode())
                        .s("thumb")
                        .s(arg.get("tim") + extension)
                        .url();
            }
        });
        setActions(new VichanActions(this, getProxiedOkHttpClientLazy(), getSiteManager(), getReplyManagerLazy()));
        setApi(new VichanApi(getSiteManager(), getBoardManager(), this));
        setParser(new VichanCommentParser());
    }

    @NotNull
    @Override
    public CommentParserType commentParserType() {
        return CommentParserType.VichanParser;
    }

    @NonNull
    @Override
    public ChunkDownloaderSiteProperties getChunkDownloaderSiteProperties() {
        return chunkDownloaderSiteProperties;
    }
}
