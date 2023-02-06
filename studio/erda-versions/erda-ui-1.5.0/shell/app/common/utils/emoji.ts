// Copyright (c) 2021 Terminus, Inc.
//
// This program is free software: you can use, redistribute, and/or modify
// it under the terms of the GNU Affero General Public License, version 3
// or later ("AGPL"), as published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program. If not, see <http://www.gnu.org/licenses/>.

export const emojiMap = {
  ':tada:': '🎉',
  ':bookmark:': '🔖',
  ':sparkles:': '✨',
  ':bug:': '🐞',
  ':card_index:': '📇',
  ':books:': '📚',
  ':bulb:': '💡',
  ':racehorse:': '🐎',
  ':lipstick:': '💄',
  ':rotating_light:': '🚨',
  ':white_check_mark:': '✅',
  ':heavy_check_mark:': '✔️',
  ':zap:': '⚡️',
  ':art:': '🎨',
  ':hammer:': '🔨',
  ':fire:': '🔥',
  ':green_heart:': '💚',
  ':lock:': '🔒',
  ':arrow_up:': '⬆️',
  ':arrow_down:': '⬇️',
  ':shirt:': '👕',
  ':alien:': '👽',
  ':pencil:': '📝',
  ':ambulance:': '🚑',
  ':rocket:': '🚀',
  ':apple:': '🍎',
  ':penguin:': '🐧',
  ':checkered_flag:': '🏁',
  ':construction:': '🚧',
  ':construction_worker:': '👷',
  ':chart_with_upwards_trend:': '📈',
  ':heavy_minus_sign:': '➖',
  ':heavy_plus_sign:': '➕',
  ':whale:': '🐳',
  ':wrench:': '🔧',
  ':package:': '📦',
  ':twisted_rightwards_arrows:': '🔀',
  ':hankey:': '💩',
  ':rewind:': '⏪',
  ':boom:': '💥',
  ':ok_hand:': '👌',
  ':wheelchair:': '♿️',
  ':truck:': '🚚',
};

export const replaceEmoji = (str: string) => {
  if (typeof str !== 'string') {
    return str;
  }

  const matches = str.match(/:[a-z_]+:/g) || [];
  let copy = str;
  matches.forEach((m) => {
    copy = copy.replace(m, emojiMap[m]);
  });
  return copy;
};
